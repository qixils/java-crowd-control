package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.ServiceManager;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client that connects to a video game hosting a Crowd Control server using the
 * {@code SimpleTCPClientConnector} and dispatches {@link Request}s.
 */
public final class SimulatedClient implements AutomatableService<Response>, ServiceManager {
	private static final Logger logger = Logger.getLogger("CC-Simul-Client");
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
	 */
	public SimulatedClient(@NotNull String ip, int port, @NotNull String password) {
		this.ip = ExceptionUtil.validateNotNull(ip, "ip");
		this.port = port;
		this.password = ServiceManager.encryptPassword(password);
	}

	@NotNull
	@Override
	@NonBlocking
	public String getIP() {
		return ip;
	}

	@Override
	@NonBlocking
	public int getPort() {
		return port;
	}

	@NotNull
	@Override
	@NonBlocking
	public String getPassword() {
		return password;
	}

	@Override
	@Blocking
	public void start() throws IOException {
		Socket socket = new Socket(ip, port);
		logger.info("Connected to " + ip + ":" + port);
		handler = new RequestHandler(socket, this, password);
		handler.start();
	}

	@Override
	@NonBlocking
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
				while (handler != null && handler.isRunning()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {
					}
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to connect to server", e);
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
	public boolean isRunning() {
		return running && handler != null && handler.isRunning();
	}

	@Override
	public boolean isAcceptingRequests() {
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return isRunning() && handler.isAcceptingRequests();
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	@Override
	public @NotNull TriState isEffectAvailable(@NotNull String effect) {
		if (handler == null) return TriState.UNKNOWN;
		return handler.isEffectAvailable(ExceptionUtil.validateNotNull(effect, "effect"));
	}

	@Override
	@Blocking
	public void shutdown() {
		if (!running) return;
		running = false;
		if (handler != null) handler.shutdown();
	}

	@Override
	@NonBlocking
	public @NotNull Flux<@NotNull Response> sendRequest(Request.@NotNull Builder builder, @Nullable Duration timeout) throws IllegalStateException {
		if (!isAcceptingRequests())
			throw new IllegalStateException("Cannot send requests while not accepting requests");
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return handler.sendRequest(builder, timeout);
	}
}
