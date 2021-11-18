package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import org.jetbrains.annotations.NotNull;

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
	private final CrowdControl crowdControl;
	private final Executor effectPool = Executors.newCachedThreadPool();
	private Socket socket;
	private volatile boolean running = true;
	private static final Logger logger = Logger.getLogger("CC-ClientSocket");
	private int sleep = 1;
	private boolean connected = false;

	/**
	 * Creates a new client-side socket manager. This is intended only for use by the library.
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
			} catch (IOException e) {
				// ensure socket is closed
				if (socket != null && !socket.isClosed())
					try {socket.close();} catch (IOException ignored) {}

				if (!running)
					continue;

				socket = null;
				String error = connected ? "Socket loop encountered an error" : "Could not connect to the Crowd Control server";
				Throwable exc = connected ? e : null;
				logger.log(Level.WARNING, error + ". Reconnecting in " + sleep + "s", exc);
				try {
					//noinspection BusyWait
					Thread.sleep(sleep * 1000L);
				} catch (InterruptedException ignored) {}
				sleep *= 2;
			}
		}
	}

	public void shutdown() throws IOException {
		running = false;
		if (socket != null && !socket.isClosed())
			socket.close();
	}
}
