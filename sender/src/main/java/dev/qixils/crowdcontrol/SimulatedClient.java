package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client that connects to a video game hosting a Crowd Control server using the
 * {@code SimpleTCPClientConnector} and dispatches {@link Request}s.
 */
public final class SimulatedClient implements AutomatableService, ServiceManager {
	private static final Logger logger = Logger.getLogger("CC-Simul-Client");
	private final String ip;
	private final int port;
	private final String password;
	private RequestHandler handler = null;
	private boolean running = true;

	public SimulatedClient(@NotNull String ip, int port, @NotNull String password) {
		this.ip = ip;
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
	public void connect() throws IOException {
		Socket socket = new Socket(ip, port);
		logger.info("Connected to " + ip + ":" + port);
		handler = new RequestHandler(socket, password);
	}

	@Override
	@NonBlocking
	public void start() {
		new Thread(this::autoReconnect).start();
	}

	@Blocking
	private void autoReconnect() {
		int reconnectionAttempts = 0;
		while (running) {
			try {
				connect();
				reconnectionAttempts = 0;
				while (handler.isRunning()) {
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
	@Blocking
	public void shutdown() {
		running = false;
		handler.shutdown();
	}

	@Override
	@NonBlocking
	public @NotNull Mono<@NotNull Response> sendRequest(@NotNull Builder builder, boolean timeout) throws IllegalStateException {
		return handler.sendRequest(builder, true);
	}
}
