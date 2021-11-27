package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import dev.qixils.crowdcontrol.socket.Response.PacketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the connection to the Crowd Control server.
 */
public final class ClientSocketManager implements SocketManager {
	private final @NotNull CrowdControl crowdControl;
	private final @NotNull Executor effectPool = Executors.newCachedThreadPool();
	private @Nullable Socket socket;
	private volatile boolean running = true;
	private static final @NotNull Logger logger = Logger.getLogger("CC-ClientSocket");
	private int sleep = 1;
	private boolean connected = false;
	private volatile boolean disconnectMessageSent = false;

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
				writeResponse(dummyShutdownResponse(null, "Server is shutting down"));
			} catch (IOException e) {
				if ("Connection reset".equals(e.getMessage())) {
					logger.info("Server terminated connection");
				} else if (socket != null && !socket.isClosed()) {
					// send error message
					writeResponse(dummyShutdownResponse(null, running ? "Server encountered an error" : "Server is shutting down"));

					// ensure socket is closed
					try {
						socket.close();
					} catch (IOException ignored) {}
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
				} catch (InterruptedException ignored) {}
				sleep *= 2;
			}
		}
	}

	private String dummyShutdownResponse(@Nullable Request cause, @Nullable String reason) {
		DummyResponse response = new DummyResponse();
		if (cause != null)
			response.id = cause.getId();
		response.message = Objects.requireNonNullElse(reason, "Disconnected");
		response.type = PacketType.DISCONNECT;
		return response.toJSON();
	}

	void writeResponse(@NotNull String response) {
		if (socket == null || socket.isClosed()) return;
		try {
			OutputStream output = socket.getOutputStream();
			output.write(response.getBytes(StandardCharsets.UTF_8));
			output.write(0x00);
			output.flush();
		} catch (IOException ignored) {}
	}

	@Override
	public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
		if (!disconnectMessageSent) {
			disconnectMessageSent = true;
			writeResponse(dummyShutdownResponse(cause, reason));
		}
		rawShutdown();
	}

	private void rawShutdown() throws IOException {
		running = false;
		if (socket != null && !socket.isClosed())
			socket.close();
	}
}
