package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// TODO: remove
final class DummyResponse implements JsonObject {
	int id = 0;
	Response.ResultType status;
	String message;
	long timeRemaining;
	Response.PacketType type;

	static void write(@Nullable Socket socket, @NotNull String message) {
		ExceptionUtil.validateNotNull(message, "message");
		if (socket == null || socket.isClosed()) return;
		try {
			OutputStream output = socket.getOutputStream();
			output.write(message.getBytes(StandardCharsets.UTF_8));
			output.write(0x00);
			output.flush();
		} catch (IOException ignored) {
		}
	}

	static DummyResponse from(@Nullable Request cause, @Nullable String reason) {
		DummyResponse response = new DummyResponse();
		if (cause != null)
			response.id = cause.getId();
		response.message = Objects.requireNonNullElse(reason, "Disconnected");
		response.type = Response.PacketType.DISCONNECT;
		return response;
	}

	public @NotNull String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}

	void write(@Nullable Socket socket) {
		write(socket, toJSON());
	}
}
