package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.ServiceManager;
import dev.qixils.crowdcontrol.TriState;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A server that can connect to multiple video games running a Crowd Control client using the
 * {@code SimpleTCPConnector} and dispatches {@link Request}s.
 */
public final class SimulatedServer implements StartableService<@NotNull Flux<@NotNull Response>>, ServiceManager {
	private static final Logger logger = Logger.getLogger("CC-Simul-Server");
	private final int port;
	private final List<RequestHandler> rawHandlers = Collections.synchronizedList(new ArrayList<>(1));
	private @Nullable ServerSocket serverSocket;
	private @Nullable Thread loopThread = null;
	private volatile boolean running = true;

	/**
	 * Creates a new {@link SimulatedServer} that listens on the given port.
	 *
	 * @param port port to listen on
	 */
	public SimulatedServer(int port) {
		this.port = port;
	}

	@Nullable
	@Override
	public String getIP() {
		return null;
	}

	@Override
	public int getPort() {
		return serverSocket != null ? serverSocket.getLocalPort() : port;
	}

	@Nullable
	@Override
	public String getPassword() {
		return null;
	}

	private List<RequestHandler> getHandlers() {
		rawHandlers.removeIf(handler -> !handler.isRunning());
		return new ArrayList<>(rawHandlers);
	}

	@Override
	public void start() throws IOException {
		serverSocket = new ServerSocket(port);
		loopThread = new Thread(this::loop);
		loopThread.start();
		logger.info("Listening on port " + port);
	}

	@Blocking
	private void loop() {
		while (running && serverSocket != null && !serverSocket.isClosed()) {
			try {
				Socket socket = serverSocket.accept();
				if (!running) {
					socket.close();
					return;
				}
				logger.info("Accepted connection from " + socket.getInetAddress());
				RequestHandler handler = new RequestHandler(socket, this, null);
				handler.start();
				rawHandlers.add(handler);
			} catch (IOException e) {
				if (running)
					logger.log(Level.WARNING, "Failed to accept connection", e);
			}
		}
		shutdown(); // something went wrong; close the server
	}

	@Override
	@CheckReturnValue
	public boolean isRunning() {
		return running
				&& serverSocket != null
				&& !serverSocket.isClosed()
				&& loopThread != null
				&& loopThread.isAlive();
	}

	@Override
	public boolean isAcceptingRequests() {
		return isRunning() && !getHandlers().isEmpty();
	}

	@Override
	public @NotNull TriState isEffectAvailable(@NotNull String effect) {
		// the values returned by this are kinda weird, but it should be fine
		boolean available = false;
		for (RequestHandler handler : getHandlers()) {
			if (handler.isEffectAvailable(effect) == TriState.UNKNOWN)
				return TriState.UNKNOWN;
			if (handler.isEffectAvailable(effect) == TriState.TRUE) {
				available = true;
				break;
			}
		}
		return TriState.fromBoolean(available);
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	/**
	 * Gets the number of clients connected to this server.
	 *
	 * @return connected clients
	 */
	public int getConnectedClients() {
		return getHandlers().size();
	}

	@Override
	@Blocking
	public void shutdown() {
		if (!running) return;
		running = false;
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to close server socket", e);
			}
		}
		for (RequestHandler handler : getHandlers()) {
			handler.shutdown();
		}
	}

	@Override
	public @NotNull Flux<@NotNull Flux<@NotNull Response>> sendRequest(Request.@NotNull Builder request, boolean timeout) throws IllegalStateException {
		if (!isAcceptingRequests())
			throw new IllegalStateException("Server is not accepting requests");

		List<RequestHandler> handlers = getHandlers();
		List<Flux<Response>> responses = new ArrayList<>(handlers.size());
		for (RequestHandler handler : handlers) {
			responses.add(handler.sendRequest(request, timeout));
		}
		return Flux.fromIterable(responses);
	}
}
