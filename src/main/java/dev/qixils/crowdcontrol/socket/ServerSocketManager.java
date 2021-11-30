package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the connection to Crowd Control clients.
 */
public final class ServerSocketManager implements SocketManager {
	private static final Logger logger = Logger.getLogger("CC-ServerSocket");
	final CrowdControl crowdControl;
	final Executor effectPool = Executors.newCachedThreadPool();
	private final List<SocketThread> socketThreads = new ArrayList<>();
	volatile boolean running = true;
	private ServerSocket serverSocket;

	/**
	 * Creates a new server-side socket manager. This is intended only for use by the library.
	 *
	 * @param crowdControl Crowd Control instance
	 */
	@CheckReturnValue
	public ServerSocketManager(@NotNull CrowdControl crowdControl) {
		this.crowdControl = crowdControl;
		new Thread(this::loop, "crowd-control-socket-loop").start();
	}

	private void loop() {
		if (!running)
			return;

		try {
			serverSocket = new ServerSocket(crowdControl.getPort());
		} catch (IOException exc) {
			logger.log(Level.SEVERE, "Could not register port " + crowdControl.getPort() + ". This is a fatal exception; attempts to reconnect will not be made.", exc);
			return;
		}

		while (running) {
			socketThreads.removeIf(SocketThread::isSocketClosed);
			try {
				Socket clientSocket = serverSocket.accept();
				SocketThread socketThread = new SocketThread(this, clientSocket);
				if (!running) {
					socketThread.shutdown(null, "Server is shutting down");
					break;
				}
				socketThreads.add(socketThread);
				socketThread.start();
			} catch (IOException exc) {
				if (running)
					logger.log(Level.WARNING, "Failed to accept new socket connection", exc);
			}
		}
	}

	@Override
	public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
		if (!running) return;
		running = false;
		for (SocketThread socketThread : socketThreads) {
			if (socketThread.isSocketActive())
				socketThread.shutdown(cause, reason);
		}
		if (serverSocket != null && !serverSocket.isClosed())
			serverSocket.close();
	}
}
