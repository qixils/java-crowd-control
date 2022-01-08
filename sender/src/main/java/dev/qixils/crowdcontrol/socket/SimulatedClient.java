package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.AutomatableService;
import dev.qixils.crowdcontrol.ServiceManager;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NonBlockingExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A client that connects to a video game hosting a Crowd Control server using the
 * {@code SimpleTCPClientConnector} and dispatches {@link Request}s.
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public final class SimulatedClient implements AutomatableService<Response>, ServiceManager {
	private static final Logger logger = LoggerFactory.getLogger("CC-Simul-Client");
	private final @NonBlockingExecutor ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final String ip;
	private final int port;
	private final String password;
	private @Nullable RequestHandler handler = null;
	private boolean running = true;
	private int reconnectionAttempts = 0;
	private ScheduledFuture<?> reconnectHandle = null;

	/**
	 * Creates a new {@code SimulatedClient} that connects to the given host using the
	 * provided password.
	 *
	 * @param ip       the IP to connect to
	 * @param port     the port to connect to
	 * @param password the password to use
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public SimulatedClient(@NotNull String ip, int port, @NotNull String password) {
		this.ip = ExceptionUtil.validateNotNull(ip, "ip");
		this.port = port;
		this.password = ServiceManager.encryptPassword(password);
	}

	@NotNull
	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public String getIP() {
		return ip;
	}

	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public int getPort() {
		return port;
	}

	@NotNull
	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public String getPassword() {
		return password;
	}

	@Override
	@Blocking
	@ApiStatus.AvailableSince("3.3.0")
	public void start() throws IOException {
		Socket socket = new Socket(ip, port);
		logger.info("Connected to " + ip + ":" + port);
		handler = new RequestHandler(socket, this, password);
		handler.start();
	}

	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	// TODO: unit test this
	public void autoStart() {
		if (!running)
			throw new IllegalStateException("Client has already shut down");
		if (reconnectHandle != null)
			throw new IllegalStateException("Client is already running");
		executor.execute(this::loop);
	}

	@Blocking
	private void loop() {
		if (!running)
			return;

		// TODO: return value of start(); should be a Mono<Void> that completes on shut down
		// or, for 3.4.0 or 4.0.0, a Flux of software states (i.e. CONNECTED, LISTENING, SHUTTING_DOWN)
		// these would help avoid this terrible if block:
		if (handler != null && !handler.isRunning()) {
			reconnectionAttempts = 0;
			reconnectHandle = executor.schedule(this::loop, 1L, TimeUnit.SECONDS);
			return;
		}

		try {
			start();
			reconnectionAttempts = 0;
			reconnectHandle = executor.schedule(this::loop, 2L, TimeUnit.SECONDS);
		} catch (IOException e) {
			logger.warn("Failed to connect to server", e);
			reconnectHandle = executor.schedule(this::loop, (long) Math.pow(2, reconnectionAttempts++), TimeUnit.SECONDS);
		}
	}

	@Override
	@CheckReturnValue
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isRunning() {
		return running && handler != null && handler.isRunning();
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isAcceptingRequests() {
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return isRunning() && handler.isAcceptingRequests();
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isShutdown() {
		return !running;
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public @NotNull TriState isEffectAvailable(@NotNull String effect) {
		if (handler == null) return TriState.UNKNOWN;
		return handler.isEffectAvailable(ExceptionUtil.validateNotNull(effect, "effect"));
	}

	@Override
	@Blocking
	@ApiStatus.AvailableSince("3.3.0")
	public void shutdown() {
		if (!running) return;
		running = false;
		if (handler != null) handler.shutdown();
	}

	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public @NotNull Flux<@NotNull Response> sendRequest(Request.@NotNull Builder builder, @Nullable Duration timeout) throws IllegalStateException {
		if (!isAcceptingRequests())
			throw new IllegalStateException("Cannot send requests while not accepting requests");
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return handler.sendRequest(builder, timeout);
	}
}
