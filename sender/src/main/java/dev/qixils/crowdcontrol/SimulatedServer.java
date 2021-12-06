package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A server that can connect to multiple video games running a Crowd Control client using the
 * {@code SimpleTCPConnector} and dispatches {@link Request}s.
 */
public class SimulatedServer implements StartableService<Flux<Response>>, ServiceManager {
	private static final Logger logger = Logger.getLogger("CC-Simul-Server");
	private final int port;
	private final List<RequestHandler> rawHandlers = new ArrayList<>(1);
	private ServerSocket serverSocket;
	private Thread loopThread = null;
	private volatile boolean running = true;

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
		synchronized (rawHandlers) {
			rawHandlers.removeIf(handler -> !handler.isRunning());
			return rawHandlers;
		}
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
		while (running && !serverSocket.isClosed()) {
			try {
				Socket socket = serverSocket.accept();
				if (!running) {
					socket.close();
					return;
				}
				logger.info("Accepted connection from " + socket.getInetAddress());
				RequestHandler handler = new RequestHandler(socket, this, null);
				handler.start();
				synchronized (rawHandlers) {
					rawHandlers.add(handler);
				}
			} catch (IOException e) {
				if (running)
					logger.log(Level.WARNING, "Failed to accept connection", e);
			}
		}
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
	public @NotNull Flux<@NotNull Response> sendRequest(@NotNull Builder request, boolean timeout) throws IllegalStateException {
		if (!isAcceptingRequests())
			throw new IllegalStateException("Server is not accepting requests");

		List<RequestHandler> handlers = getHandlers();
		List<Mono<Response>> responses = new ArrayList<>(handlers.size());
		for (RequestHandler handler : handlers) {
			responses.add(handler.sendRequest(request, false));
		}
		Flux<Response> flux = Flux.merge(responses);
		if (timeout)
			flux = flux.timeout(TIMEOUT);
		return flux;
	}
}
