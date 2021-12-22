package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles the connection to a Crowd Control client when operating in server mode.
 */
final class SocketThread extends Thread implements SocketManager {
	private static final @NotNull String RAW_PASSWORD_REQUEST;
	private static final byte @NotNull [] PASSWORD_REQUEST;
	private static final @NotNull Logger logger = LoggerFactory.getLogger("CC-SocketThread");

	static {
		DummyResponse resp = new DummyResponse();
		resp.type = Response.PacketType.LOGIN;
		RAW_PASSWORD_REQUEST = resp.toJSON();
		byte[] json = RAW_PASSWORD_REQUEST.getBytes(StandardCharsets.UTF_8);
		// array copy adds an extra 0x00 byte to the end, indicating the end of the packet
		PASSWORD_REQUEST = Arrays.copyOf(json, json.length + 1);
	}

	final @NotNull ServerSocketManager socketManager;
	final @NotNull Socket socket;
	final @NotNull String displayName = UUID.randomUUID().toString().substring(30).toUpperCase(Locale.ENGLISH);
	private volatile boolean running = true;

	SocketThread(@NotNull ServerSocketManager socketManager, @NotNull Socket clientSocket) {
		this.socketManager = socketManager;
		this.socket = clientSocket;
	}

	public void run() {
		logger.info("Successfully connected to a new client (" + displayName + ")");

		try {
			EffectExecutor effectExecutor = new EffectExecutor(this);

			// prompt client for password
			OutputStream output = socket.getOutputStream();
			output.write(PASSWORD_REQUEST);
			output.flush();

			while (running) {
				effectExecutor.run();
			}

			logger.info("Disconnecting from client socket (" + displayName + ")");
			DummyResponse.from(null, "Server is shutting down").write(socket);
		} catch (IOException exc) {
			if ("Connection reset".equals(exc.getMessage())) {
				logger.info("Client disconnected from server (" + displayName + ")");
				return;
			}

			// send disconnection message to socket & ensure socket is closed
			try {
				shutdown(null, running ? "Server encountered an error" : "Server is shutting down");
			} catch (IOException ignored) {
			}

			// log disconnection
			if (running)
				logger.warn("Erroneously disconnected from client socket (" + displayName + ")", exc);
			else
				logger.info("Client socket shutting down (" + displayName + ")");
		}
	}

	public boolean isSocketActive() {
		return running && !socket.isClosed();
	}

	public boolean isSocketClosed() {
		return !isSocketActive();
	}

	@Override
	public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
		if (!running) return;
		running = false;
		if (!socket.isClosed()) {
			DummyResponse.from(cause, reason).write(socket);
			socket.close();
		}
	}
}
