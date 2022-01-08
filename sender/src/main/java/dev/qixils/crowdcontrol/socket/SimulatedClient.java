package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.AutomatableService;
import dev.qixils.crowdcontrol.ServiceManager;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;

/**
 * A client that connects to a video game hosting a Crowd Control server using the
 * {@code SimpleTCPClientConnector} and dispatches {@link Request}s.
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public final class SimulatedClient implements AutomatableService<Response>, ServiceManager {
	private static final Logger logger = LoggerFactory.getLogger("CC-Simul-Client");
	private final String ip;
	private final int port;
	private final String password;
	private @Nullable RequestHandler handler = null;
	private boolean running = true;

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
	public void autoStart() {
		new Thread(this::autoReconnect).start();
	}

	@Blocking
	private void autoReconnect() {
		int reconnectionAttempts = 0;
		while (running) {
			try {
				start();
				reconnectionAttempts = 0;
				try {
					// TODO: avoid busy-waiting
					Thread.sleep(2000); // wait for service to start up
					while (handler != null && handler.isRunning()) {
						Thread.sleep(1000); // wait for service to shut down
					}
				} catch (InterruptedException ignored) {
				}
			} catch (IOException e) {
				logger.warn("Failed to connect to server", e);
				// exponential backoff
				try {
					Thread.sleep(1000 * (long) Math.pow(2, reconnectionAttempts++));
				} catch (InterruptedException ignored) {
				}
			}
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
