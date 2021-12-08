package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonParseException;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.CrowdControlException;
import dev.qixils.crowdcontrol.exceptions.EffectUnavailableException;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Request.Type;
import dev.qixils.crowdcontrol.socket.Response.PacketType;
import dev.qixils.crowdcontrol.socket.Response.ResultType;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RequestHandler implements SimulatedService<Response> {
	private static final Logger logger = Logger.getLogger("CC-RequestHandler");
	private static final Executor executor = Executors.newCachedThreadPool();
	private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	private final Map<Integer, EffectData> effectDataMap = new ConcurrentHashMap<>(1);
	private final Map<String, Boolean> effectAvailabilityMap = new ConcurrentHashMap<>(1);
	private final Socket socket;
	private final SimulatedService<?> parent;
	private final InputStreamReader inputStream;
	private final OutputStream outputStream;
	private final @Nullable String encryptedPassword;
	private final Thread loopThread;
	private boolean running = true;
	private int nextRequestId = 0;
	private boolean loggedIn;

	RequestHandler(@NotNull Socket socket, @NotNull SimulatedService<?> parent, @Nullable String encryptedPassword) throws IOException {
		this.socket = socket;
		this.parent = parent;
		this.inputStream = new InputStreamReader(socket.getInputStream());
		this.outputStream = socket.getOutputStream();
		this.encryptedPassword = encryptedPassword;
		loggedIn = encryptedPassword == null;
		loopThread = new Thread(this::loop);
	}

	public void start() throws IllegalThreadStateException {
		logger.info("Starting request handler");
		loopThread.start();
	}

	@Override
	public void shutdown() {
		if (!running) return;
		running = false;
		logger.info("Shutting down request handler");
		try {
			socket.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to close socket", e);
		}
		parent.shutdown();

		// wait for request streams to finish
		scheduledExecutor.schedule(() -> {
			effectDataMap.forEach(($, data) -> data.sink.error(new CrowdControlException("RequestHandler shutting down")));
			effectDataMap.clear();
		}, 1, TimeUnit.SECONDS);
	}

	@Override
	public boolean isRunning() {
		return running && !socket.isClosed() && loopThread.isAlive();
	}

	@Override
	public boolean isAcceptingRequests() {
		return isRunning() && loggedIn;
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	@Override
	public @NotNull TriState isEffectAvailable(@NotNull String effect) {
		return TriState.fromBoolean(effectAvailabilityMap.get(effect));
	}

	@Blocking
	private void loop() {
		try {
			while (running) {
				Response response;
				try {
					response = JsonObject.fromInputStream(inputStream, Response::fromJSON);
				} catch (JsonParseException e) {
					logger.log(Level.WARNING, "Failed to parse JSON from socket", e);
					return;
				}

				if (response == null) {
					// assuming socket is closed because we received an empty packet
					shutdown();
					return;
				}

				switch (response.getPacketType()) {
					case LOGIN:
						if (encryptedPassword == null)
							throw new IllegalStateException("Service sent LOGIN packet, but no password was provided");

						logger.info("Login prompted; sending password");
						sendRequest(new Request.Builder()
								.type(Type.LOGIN)
								.message(encryptedPassword), true)
								.subscribe();
						break;

					case DISCONNECT:
						logger.warning("Disconnected from service: " + response.getMessage());
						shutdown();
						break;

					case LOGIN_SUCCESS:
						logger.info("Login successful");
						loggedIn = true;
						break;

					case EFFECT_RESULT:
						EffectData data = effectDataMap.get(response.getId());
						if (data == null) {
							logger.warning("Received response for unknown request ID: " + response.getId());
							break;
						}
						logger.fine("Received response for request " + response.getId());

						data.responseReceived = true;
						data.sink.next(response); // send response to subscriber

						// set availability of effect
						String effectName = data.request.getEffect();
						if (!effectAvailabilityMap.containsKey(effectName))
							effectAvailabilityMap.put(effectName, response.getResultType() != ResultType.UNAVAILABLE);

						boolean toSchedule = false; // whether to schedule a fake FINISHED response
						// handle the various possible response types/states
						if (response.isTerminating()) {
							data.sink.complete();
						} else if (response.getResultType() == ResultType.RETRY) {
							int retryDelay = data.getRetryDelay();
							if (retryDelay == -1)
								data.sink.complete();
							else
								scheduledExecutor.schedule(
										() -> writeRequest(data.request, data.sink),
										retryDelay, TimeUnit.SECONDS
								);
						} else if (response.getResultType() == ResultType.PAUSED) {
							data.pause();
						} else if (response.getResultType() == ResultType.RESUMED) {
							data.resume();
							toSchedule = true;
						} else if (response.getResultType() == ResultType.SUCCESS) {
							// this represents the start of a timed effect
							// because this was not caught by the first if block
							data.updateTimeRemaining(response.getTimeRemaining());
							toSchedule = true;
						}

						if (toSchedule)
							data.scheduledFuture = scheduledExecutor.schedule(
									() -> {
										// send fake FINISHED packet to flux stream
										data.sink.next(new Response(
												response.getId(),
												null,
												PacketType.EFFECT_RESULT,
												ResultType.FINISHED,
												null,
												0
										));
										// complete flux stream
										data.sink.complete();
									},
									response.getTimeRemaining(),
									TimeUnit.MILLISECONDS
							);
				}
			}
		} catch (IOException e) {
			if (running)
				logger.log(Level.SEVERE, "Failed to read from socket", e);
		}
	}

	@Override
	public @NotNull Flux<@NotNull Response> sendRequest(@NotNull Builder builder, boolean timeout) throws IllegalStateException {
		Objects.requireNonNull(builder, "builder cannot be null");
		Type type = builder.type();
		if (type == null)
			throw new IllegalArgumentException("Request type is null");

		// create request (done first to ensure it is valid (i.e. doesn't throw))
		Request request = builder.id(++nextRequestId).build();

		// ensure effect is available
		if (type.isEffectType() && isEffectAvailable(request.getEffect()) == TriState.FALSE)
			throw new EffectUnavailableException("Effect " + request.getEffect() + " is known to be unavailable to this service");

		// TODO: unit test
		return Flux.<Response>create(sink -> {
			// ensure service is accepting requests
			if (type.isEffectType() && !isAcceptingRequests()) {
				sink.error(new IllegalStateException("RequestHandler is not accepting requests"));
				return;
			}

			// add sink to map of publishers
			EffectData data = new EffectData(request.getId(), request, sink);
			effectDataMap.put(request.getId(), data);

			// manage responseReceivedMap for timeout functionality
			if (timeout) {
				scheduledExecutor.schedule(() -> {
					if (!isAcceptingRequests()) return;
					if (data.responseReceived) return;
					final String error = "Timed out waiting for response for request " + request.getId();
					logger.fine(error);
					sink.error(new TimeoutException(error));
				}, TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			}

			// send request
			executor.execute(() -> writeRequest(request, sink));
			// TODO: timeout unit test
		}).doOnComplete(() -> effectDataMap.remove(request.getId()));
	}

	private void writeRequest(@NotNull Request request, @NotNull FluxSink<Response> sink) {
		assert isAcceptingRequests() || (isRunning() && !request.getType().isEffectType());
		try {
			outputStream.write(request.toJSON().getBytes(StandardCharsets.UTF_8));
			outputStream.write(0x00);
			outputStream.flush();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to send request", e);
			sink.error(e);
		}
	}

	private static final class EffectData {
		private final int id;
		private final @NotNull Request request;
		private final @NotNull FluxSink<@NotNull Response> sink;
		private boolean responseReceived = false;
		private int retryCount = 0;
		private long timeRemaining = 0;
		private long timeUpdatedAt = 0;
		private boolean paused = false;
		private ScheduledFuture<?> scheduledFuture;

		private EffectData(int id, @NotNull Request request, @NotNull FluxSink<@NotNull Response> sink) {
			this.id = id;
			this.request = request;
			this.sink = sink;
		}

		private int getRetryDelay() {
			if (retryCount > 6) return -1;
			return (int) Math.pow(2, 2 + (retryCount++));
		}

		private void updateTimeRemaining(long timeRemaining) {
			this.timeRemaining = timeRemaining;
			this.timeUpdatedAt = System.currentTimeMillis();
		}

		// get the elapsed time since the last update
		private long getCurrentTimeRemaining() {
			return Math.max(0, timeRemaining - (System.currentTimeMillis() - timeUpdatedAt));
		}

		private void pause() {
			if (paused) return;
			paused = true;
			if (scheduledFuture != null)
				scheduledFuture.cancel(false);
			scheduledFuture = null;
			// update the time remaining using the elapsed time since the last update
			updateTimeRemaining(getCurrentTimeRemaining());
		}

		private void resume() {
			if (!paused) return;
			paused = false;
			timeUpdatedAt = System.currentTimeMillis();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			EffectData that = (EffectData) o;
			return id == that.id && responseReceived == that.responseReceived && retryCount == that.retryCount && sink.equals(that.sink);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, sink, responseReceived, retryCount);
		}
	}
}
