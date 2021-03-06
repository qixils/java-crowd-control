package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class ServerResponse extends Response {
	private final transient @NotNull ServerSocketManager manager;

	ServerResponse(int id, @NotNull ServerSocketManager manager, @Nullable PacketType packetType, @Nullable ResultType type, @Nullable String message, long timeRemaining) throws IllegalArgumentException {
		super(id, null, packetType, type, message, timeRemaining);
		this.manager = manager;
	}

	private ServerResponse(@NotNull Builder builder) {
		this(
				builder.id(),
				builder.manager,
				builder.packetType(),
				builder.type(),
				builder.message(),
				builder.timeRemaining()
		);
	}

	@Override
	public Response.@NotNull Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public boolean isOriginKnown() {
		return true;
	}

	@Override
	void rawSend() throws IllegalStateException, IOException {
		List<SocketThread> threads = manager.getSocketThreads();
		List<IOException> exceptions = new ArrayList<>(threads.size());
		for (SocketThread thread : threads) {
			try {
				new Response.Builder(this).originatingSocket(thread.socket).build().rawSend();
			} catch (IOException e) {
				exceptions.add(e);
			}
		}
		if (!exceptions.isEmpty())
			throw new AggregatedIOException(exceptions);
	}

	static final class Builder extends Response.Builder {
		private final @NotNull ServerSocketManager manager;

		Builder(int id, @NotNull ServerSocketManager manager) {
			super(id, null);
			this.manager = manager;
		}

		private Builder(@NotNull ServerResponse response) {
			super(response);
			this.manager = response.manager;
		}

		private Builder(@NotNull Builder builder) {
			super(builder);
			this.manager = builder.manager;
		}

		@Override
		public Response.@NotNull Builder clone() {
			return new Builder(this);
		}

		@Override
		public @NotNull Response build() {
			if (originatingSocket() == null)
				return new ServerResponse(this);
			else
				return super.build();
		}
	}

	private static final class AggregatedIOException extends IOException {
		private AggregatedIOException(@NotNull List<IOException> exceptions) {
			super("Multiple exceptions occurred while sending the response to the connected client(s): " +
					exceptions.stream()
							.map(IOException::getMessage)
							.distinct()
							.collect(Collectors.joining(", ")));
		}
	}
}
