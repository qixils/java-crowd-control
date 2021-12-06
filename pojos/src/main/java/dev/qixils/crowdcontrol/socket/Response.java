package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An outgoing packet to the Crowd Control TCP server carrying the result of executing
 * a {@link Request requested} effect.
 *
 * @see Request
 */
public final class Response implements JsonObject {
	private static final Logger logger = Logger.getLogger("CC-Response");
	@SerializedName("type")
	private PacketType packetType;
	private transient Request request;
	private int id;
	@SerializedName("status")
	private ResultType type;
	private String message;
	private long timeRemaining; // millis

	/**
	 * Instantiates an empty {@link Response}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	Response() {
	}

	/**
	 * Constructs a response to a {@link Request} given its ID, the result of executing the effect,
	 * and an associated message.
	 *
	 * @param request       originating request
	 * @param type          result of execution
	 * @param message       result message
	 * @param timeRemaining remaining duration for the referenced effect in milliseconds
	 * @param packetType    type of packet
	 */
	@CheckReturnValue
	Response(@NotNull Request request,
			 Response.@NotNull ResultType type,
			 @NotNull String message,
			 long timeRemaining,
			 @Nullable PacketType packetType) {
		this.request = Objects.requireNonNull(request, "request cannot be null");
		this.id = request.getId();
		this.type = Objects.requireNonNull(type, "type cannot be null");
		this.message = Objects.requireNonNull(message, "message cannot be null");
		this.timeRemaining = timeRemaining;
		this.packetType = Objects.requireNonNullElse(packetType, PacketType.EFFECT_RESULT);
	}

	/**
	 * Constructs a response to a {@link Request} given its ID, the result of executing the effect,
	 * and an associated message.
	 *
	 * @param request       originating request
	 * @param type          result of execution
	 * @param message       result message
	 * @param timeRemaining remaining duration for the referenced effect in milliseconds
	 */
	@CheckReturnValue
	public Response(@NotNull Request request,
					@NotNull Response.ResultType type,
					@NotNull String message,
					long timeRemaining) {
		this(request, type, message, timeRemaining, null);
	}

	/**
	 * Creates a {@link Response} object from JSON.
	 *
	 * @param json input json data from the Crowd Control game
	 * @return a new Response object
	 * @throws JsonSyntaxException the JSON failed to be parsed
	 */
	@NotNull
	@CheckReturnValue
	public static Response fromJSON(@NotNull String json) throws JsonSyntaxException {
		return ByteAdapter.GSON.fromJson(Objects.requireNonNull(json, "json"), Response.class);
	}

	/**
	 * Gets the unique {@link Request} that caused this response.
	 *
	 * @return original request
	 */
	public Request getRequest() {
		return request;
	}

	/**
	 * Gets the ID of the outgoing packet. Corresponds to a unique transaction.
	 *
	 * @return packet ID
	 */
	@CheckReturnValue
	public int getId() {
		return id;
	}

	/**
	 * Gets the result of executing an effect.
	 *
	 * @return effect result
	 */
	@NotNull
	@CheckReturnValue
	public Response.ResultType getResultType() {
		return type;
	}

	/**
	 * Gets the type of packet represented by this response.
	 * <p>
	 * Note: unless directly working with library internals, this will always be {@link PacketType#EFFECT_RESULT}.
	 *
	 * @return packet type
	 */
	@NotNull
	@CheckReturnValue
	public PacketType getPacketType() {
		return packetType;
	}

	/**
	 * Gets the message that will be delivered along with the result.
	 *
	 * @return result message
	 */
	@NotNull
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the milliseconds left until the referenced effect ends.
	 *
	 * @return effect duration in milliseconds
	 */
	@CheckReturnValue
	public long getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Outputs this object as a JSON string for use in the server connection.
	 *
	 * @return JSON string
	 */
	@NotNull
	@CheckReturnValue
	public String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}

	/**
	 * Gets a mutable {@link Builder} representing this Response.
	 *
	 * @return new builder
	 */
	@NotNull
	@CheckReturnValue
	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Response response = (Response) o;
		return getId() == response.getId()
				&& getTimeRemaining() == response.getTimeRemaining()
				&& getPacketType() == response.getPacketType()
				&& getResultType() == response.getResultType()
				&& Objects.equals(getMessage(), response.getMessage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPacketType(), getId(), getResultType(), getMessage(), getTimeRemaining());
	}

	/**
	 * Sends this {@link Response} to the client or server that delivered the related {@link Request}.
	 *
	 * @throws IllegalStateException the related {@link Request} does not have an associated client or server
	 */
	public void send() throws IllegalStateException {
		if (request == null || request.originatingSocket == null) {
			throw new IllegalStateException("This Response was constructed with an illegal Request which is not associated with a client or server");
		}

		if (request.originatingSocket.isClosed()) {
			return;
		}

		//object is never updated after assignment, so we can ignore this error:
		//noinspection SynchronizeOnNonFinalField
		synchronized (request.originatingSocket) {
			try {
				OutputStream output = request.originatingSocket.getOutputStream();
				output.write(toJSON().getBytes(StandardCharsets.UTF_8));
				output.write(0x00);
				output.flush();
			} catch (IOException exc) {
				logger.log(Level.WARNING, "Failed to write response to socket", exc);
			}
		}
	}

	/**
	 * Determines the type of packet being sent.
	 */
	public enum PacketType implements ByteObject {
		/**
		 * The packet is the result of executing an effect.
		 */
		EFFECT_RESULT,
		/**
		 * <b>Internal value</b> used to prompt a connecting client for a password.
		 */
		LOGIN((byte) 0xF0),
		/**
		 * <b>Internal value</b> used to indicate a successful login.
		 */
		LOGIN_SUCCESS((byte) 0xF1),
		/**
		 * <b>Internal value</b> used to indicate that the socket is being disconnected.
		 */
		DISCONNECT((byte) 0xFE),
		/**
		 * <b>Internal value</b> used to reply to a keep alive packet.
		 */
		KEEP_ALIVE((byte) 0xFF);

		private static final Map<Byte, PacketType> BY_BYTE;

		static {
			Map<Byte, PacketType> map = new HashMap<>(values().length);
			for (PacketType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final byte encodedByte;

		PacketType(byte encodedByte) {
			this.encodedByte = encodedByte;
		}

		PacketType() {
			this.encodedByte = (byte) ordinal();
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 *
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 */
		public static @Nullable PacketType from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		public byte getEncodedByte() {
			return encodedByte;
		}
	}

	/**
	 * The result of processing an incoming packet.
	 */
	public enum ResultType implements ByteObject {
		/**
		 * The effect was applied successfully.
		 */
		SUCCESS,
		/**
		 * The effect failed to be applied. Will refund the purchaser.
		 */
		FAILURE,
		/**
		 * The requested effect is unusable and should not be requested again.
		 */
		UNAVAILABLE,
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 */
		RETRY,
		/**
		 * The timed effect has been paused and is now waiting.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		PAUSED((byte) 0x06),
		/**
		 * The timed effect has been resumed and is counting down again.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		RESUMED((byte) 0x07),
		/**
		 * The timed effect has finished.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		FINISHED((byte) 0x08),
		/**
		 * Indicates that this Crowd Control server is not yet accepting requests.
		 * <p>
		 * This is an internal field used to indicate that the login process with a client has
		 * not yet completed. You should instead use {@link #FAILURE} to indicate a
		 * temporary failure or {@link #UNAVAILABLE} to indicate a permanent failure.
		 */
		NOT_READY((byte) 0xFF);

		private static final Map<Byte, ResultType> BY_BYTE;

		static {
			Map<Byte, ResultType> map = new HashMap<>(values().length);
			for (ResultType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final byte encodedByte;

		ResultType(byte encodedByte) {
			this.encodedByte = encodedByte;
		}

		ResultType() {
			this.encodedByte = (byte) ordinal();
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 *
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 */
		public static @Nullable ResultType from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		public byte getEncodedByte() {
			return encodedByte;
		}
	}

	/**
	 * Mutable builder for the immutable {@link Response} class.
	 */
	public static class Builder implements Cloneable {
		private final Request request;
		private ResultType type;
		private String message;
		private long timeRemaining;
		private PacketType packetType;

		// used to determine if a message has been manually set.
		// false means a message has either not been set or it has only been set by #type
		private boolean messageSet = false;

		/**
		 * Creates a new builder using the data from a {@link Response}.
		 *
		 * @param source source for a new builder
		 */
		@CheckReturnValue
		public Builder(@NotNull Response source) {
			Objects.requireNonNull(source, "source cannot be null");
			this.request = source.request;
			this.message = source.message;
			this.type = source.type;
			this.timeRemaining = source.timeRemaining;
			this.packetType = source.packetType;
		}

		/**
		 * Creates a new builder representing the {@link Response} to a {@link Request}.
		 *
		 * @param request request to respond to
		 */
		@CheckReturnValue
		public Builder(@NotNull Request request) {
			this.request = Objects.requireNonNull(request, "request cannot be null");
		}

		/**
		 * Sets the type of result being returned.
		 *
		 * @param type result type
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder type(@Nullable ResultType type) {
			this.type = type;
			if (type != null && !messageSet)
				message = type.name();
			return this;
		}

		/**
		 * Sets the message describing or explaining the response.
		 * <br>Useful for explaining why an effect failed to apply.
		 *
		 * @param message response message
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder message(@Nullable String message) {
			messageSet = true;
			this.message = message;
			return this;
		}

		/**
		 * Sets the time left on the referenced effect in milliseconds.
		 *
		 * @param timeRemaining time in milliseconds
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(long timeRemaining) {
			if (timeRemaining < 0)
				throw new IllegalArgumentException("'timeRemaining' must be positive");
			this.timeRemaining = timeRemaining;
			return this;
		}

		/**
		 * Sets the time left on the referenced effect.
		 *
		 * @param timeRemaining effect duration
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Duration timeRemaining) {
			return timeRemaining != null ? timeRemaining(timeRemaining.toMillis()) : this;
		}

		/**
		 * Sets the time at which the referenced effect will end.
		 *
		 * @param endEffectAt time to end effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Temporal endEffectAt) {
			return endEffectAt != null
					? timeRemaining(ChronoUnit.MILLIS.between(LocalDateTime.now(), endEffectAt))
					: this;
		}

		/**
		 * Sets the type of packet that this Response represents.
		 * <p>
		 * Note: this is intended only for internal library use.
		 *
		 * @param packetType type of packet
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder packetType(@Nullable PacketType packetType) {
			this.packetType = packetType;
			return this;
		}

		/**
		 * Builds a new {@link Response} object.
		 *
		 * @return new Response
		 */
		@NotNull
		@CheckReturnValue
		public Response build() {
			return new Response(request, type, message, timeRemaining, packetType);
		}

		/**
		 * Builds this {@link Response} and then sends it to the client or server that delivered the related {@link Request}.
		 *
		 * @throws IllegalStateException the related {@link Request} does not have an associated client or server
		 */
		public void send() {
			build().send();
		}

		/**
		 * Creates a new {@link Builder} object with the same parameters.
		 *
		 * @return cloned builder
		 */
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		public Builder clone() {
			return new Builder(request).timeRemaining(timeRemaining).message(message).type(type).packetType(packetType);
		}
	}
}
