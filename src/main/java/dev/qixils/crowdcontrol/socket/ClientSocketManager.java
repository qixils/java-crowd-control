package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the connection to the Crowd Control server.
 */
public final class ClientSocketManager implements SocketManager {
	private static final @NotNull Logger logger = Logger.getLogger("CC-ClientSocket");
	private final @NotNull CrowdControl crowdControl;
	private final @NotNull Executor effectPool = Executors.newCachedThreadPool();
	private @Nullable Socket socket;
	private volatile boolean running = true;
	private int sleep = 1;
	private boolean connected = false;

	/**
	 * Creates a new client-side socket manager. This is intended only for use by the library.
	 *
	 * @param crowdControl Crowd Control instance
	 */
	@CheckReturnValue
	public ClientSocketManager(@NotNull CrowdControl crowdControl) {
		this.crowdControl = crowdControl;
		new Thread(this::loop, "crowd-control-socket-loop").start();
	}

	private void loop() {
		while (running) {
			try {
				socket = new Socket(crowdControl.getIP(), crowdControl.getPort());
				logger.info("Connected to Crowd Control server");
				sleep = 1;
				connected = true;
				EffectExecutor effectExecutor = new EffectExecutor(
						socket,
						effectPool,
						crowdControl
				);

				while (running) {
					effectExecutor.run();
				}

				logger.info("Crowd Control socket shutting down");
				DummyResponse.from(null, "Server is shutting down").write(socket);
			} catch (IOException e) {
				if ("Connection reset".equals(e.getMessage())) {
					logger.info("Server terminated connection");
				} else if (socket != null && !socket.isClosed()) {
					// send error message
					DummyResponse.from(null, running ? "Server encountered an error" : "Server is shutting down").write(socket);

					// ensure socket is closed
					try {
						socket.close();
					} catch (IOException ignored) {
					}
				}

				if (!running)
					continue;

				// render error message with exponential decay
				socket = null;
				String error = connected ? "Socket loop encountered an error" : "Could not connect to the Crowd Control server";
				Throwable exc = connected ? e : null;
				logger.log(Level.WARNING, error + ". Reconnecting in " + sleep + "s", exc);
				try {
					//noinspection BusyWait
					Thread.sleep(sleep * 1000L);
				} catch (InterruptedException ignored) {
				}
				sleep *= 2;
			}
		}
	}

	@Override
	public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
		if (!running) return;
		running = false;
		if (socket != null && !socket.isClosed()) {
			DummyResponse.from(cause, reason).write(socket);
			socket.close();
		}
	}
}
