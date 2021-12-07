package dev.qixils.crowdcontrol;

import com.google.gson.JsonParseException;
import dev.qixils.crowdcontrol.socket.JsonObject;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Request.Type;
import dev.qixils.crowdcontrol.socket.Response;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RequestHandler implements SimulatedService<Response> {
	private static final Logger logger = Logger.getLogger("CC-RequestHandler");
	private static final Executor executor = Executors.newCachedThreadPool();
	private final Map<Integer, List<FluxSink<Response>>> publishers = new ConcurrentHashMap<>(1);
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
						List<FluxSink<Response>> sinks = publishers.get(response.getId());
						if (sinks != null) {
							logger.fine("Received response for request " + response.getId());
							// todo: handle different response types
							for (FluxSink<Response> sink : sinks) {
								sink.next(response);
								if (response.isTerminating()) {
									sink.complete();
									sinks.remove(sink);
								}
							}
						} else
							logger.warning("Received response for unknown request ID: " + response.getId());
				}
			}
		} catch (IOException e) {
			if (running)
				logger.log(Level.SEVERE, "Failed to read from socket", e);
		}
	}

	@Override
	public @NotNull Flux<@NotNull Response> sendRequest(@NotNull Builder builder, boolean timeout) throws IllegalStateException {
		Type type = builder.getType();
		if (type == null)
			throw new IllegalArgumentException("Request type is null");

		// TODO: unit test
		return Flux.create(sink -> {
			if (type.isEffectType() && !isAcceptingRequests()) {
				sink.error(new IllegalStateException("RequestHandler is not accepting requests"));
				return;
			}
			Request request = builder.id(++nextRequestId).build();
			publishers.computeIfAbsent(request.getId(), $ -> new ArrayList<>()).add(sink);
			executor.execute(() -> {
				try {
					outputStream.write(request.toJSON().getBytes(StandardCharsets.UTF_8));
					outputStream.write(0x00);
					outputStream.flush();
				} catch (Exception e) {
					logger.log(Level.WARNING, "Failed to send request", e);
					sink.error(e);
					List<FluxSink<Response>> sinks = publishers.get(request.getId());
					// this really shouldn't be null, but just in case:
					if (sinks != null)
						sinks.remove(sink);
				}
			});
			// TODO: timeout
			// TODO: timeout unit test
		});
	}
}
