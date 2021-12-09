package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
	private transient Socket originatingSocket;
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
	@SuppressWarnings("unused")
	// used by GSON
	Response() {
	}

	/**
	 * Instantiates a new {@link Response} given an ID, the {@link Socket} that originated the
	 * {@link Request}, and information about the result of the execution.
	 *
	 * @param id                ID of the {@link Request} that was executed
	 * @param originatingSocket the socket that originated the request
	 * @param packetType        type of the packet
	 * @param type              result of the execution
	 * @param message           result message
	 * @param timeRemaining     time remaining in milliseconds until the effect completes
	 *                          or {@code 0} if the effect is not time-based
	 * @throws IllegalArgumentException May be thrown in various circumstances:
	 *                                  <ul>
	 *                                      <li>if the {@code id} is negative</li>
	 *                                      <li>if the {@code timeRemaining} is negative</li>
	 *                                      <li>if the {@code packetType} is {@link PacketType#EFFECT_RESULT} and {@code type} is null</li>
	 *                                      <li>if the {@code packetType} is not {@link PacketType#EFFECT_RESULT} and {@code type} is non-null</li>
	 *                                  </ul>
	 */
	private Response(int id,
					 @Nullable Socket originatingSocket,
					 @Nullable PacketType packetType,
					 @Nullable ResultType type,
					 @Nullable String message,
					 long timeRemaining) throws IllegalArgumentException {
		this.id = id;
		if (this.id < 0)
			throw new IllegalArgumentException("ID cannot be negative");

		this.timeRemaining = timeRemaining;
		if (this.timeRemaining < 0)
			throw new IllegalArgumentException("timeRemaining cannot be negative");

		this.originatingSocket = originatingSocket;

		// validate packet type and result type
		this.packetType = Objects.requireNonNullElse(packetType, PacketType.EFFECT_RESULT);
		if (this.packetType == PacketType.EFFECT_RESULT && type == null)
			throw new IllegalArgumentException("type cannot be null if packetType is EFFECT_RESULT");
		else if (this.packetType != PacketType.EFFECT_RESULT && type != null)
			throw new IllegalArgumentException("type cannot be non-null if packetType is not EFFECT_RESULT");
		this.type = type;

		// set message
		this.message = type == null
				? message
				: Objects.requireNonNullElseGet(message, type::name);
	}

	/**
	 * Instantiates a new non-{@link PacketType#EFFECT_RESULT} {@link Response} to a
	 * {@link Request} with the given ID and result.
	 *
	 * @param id                ID of the {@link Request} that was executed
	 * @param originatingSocket the socket that originated the request
	 * @param packetType        type of the packet (must not be {@link PacketType#EFFECT_RESULT})
	 * @param message           result message
	 * @throws IllegalArgumentException May be thrown in various circumstances:
	 *                                  <ul>
	 *                                      <li>if the {@code id} is negative</li>
	 *                                      <li>if the {@code packetType} is null</li>
	 *                                      <li>if the {@code packetType} is {@link PacketType#EFFECT_RESULT}</li>
	 *                                      <li>if the {@code message} is null</li>
	 *                                  </ul>
	 */
	Response(int id,
			 @Nullable Socket originatingSocket,
			 @NotNull PacketType packetType,
			 @NotNull String message) throws IllegalArgumentException {
		if (packetType == PacketType.EFFECT_RESULT)
			throw new IllegalArgumentException("packetType cannot be EFFECT_RESULT in this constructor");
		this.id = id;
		if (this.id < 0)
			throw new IllegalArgumentException("ID cannot be negative");
		this.originatingSocket = originatingSocket;
		this.packetType = ExceptionUtil.validateNotNull(packetType, "packetType");
		this.message = ExceptionUtil.validateNotNull(message, "message");
		this.type = null;
		this.timeRemaining = 0;
	}

	/**
	 * Instantiates a new {@link Response} given an ID, the {@link Socket} that originated the
	 * {@link Request}, and information about the result of the execution.
	 *
	 * @param id                ID of the {@link Request} that was executed
	 * @param originatingSocket the socket that originated the request
	 * @param type              result of the execution
	 * @param message           result message
	 * @param timeRemaining     time remaining in milliseconds until the effect completes
	 *                          or {@code 0} if the effect is not time-based
	 * @throws IllegalArgumentException May be thrown in various circumstances:
	 *                                  <ul>
	 *                                      <li>if the {@code id} is negative</li>
	 *                                      <li>if the {@code timeRemaining} is negative</li>
	 *                                      <li>if the {@code type} is null</li>
	 *                                  </ul>
	 */
	Response(int id,
			 @Nullable Socket originatingSocket,
			 @NotNull ResultType type,
			 @Nullable String message,
			 long timeRemaining) throws IllegalArgumentException {
		this(id, originatingSocket, PacketType.EFFECT_RESULT, type, message, timeRemaining);
	}

	/**
	 * Instantiates a new non-{@link PacketType#EFFECT_RESULT} {@link Response} to a given
	 * {@link Request}.
	 *
	 * @param request    originating request
	 * @param packetType type of packet (must not be {@link PacketType#EFFECT_RESULT})
	 * @param message    result message
	 * @throws IllegalArgumentException May be thrown in various circumstances:
	 *                                  <ul>
	 *                                      <li>if the {@code request} is null</li>
	 *                                      <li>if the {@code packetType} is null</li>
	 *                                      <li>if the {@code packetType} is {@link PacketType#EFFECT_RESULT}</li>
	 *                                      <li>if the {@code message} is null</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	Response(@NotNull Request request,
			 @NotNull PacketType packetType,
			 @NotNull String message) throws IllegalArgumentException {
		if (packetType == PacketType.EFFECT_RESULT)
			throw new IllegalArgumentException("packetType cannot be EFFECT_RESULT in this constructor");
		ExceptionUtil.validateNotNull(request, "request");
		this.id = request.getId();
		this.originatingSocket = request.originatingSocket;
		this.packetType = ExceptionUtil.validateNotNull(packetType, "packetType");
		this.type = null;
		this.message = ExceptionUtil.validateNotNull(message, "message");
		this.timeRemaining = 0;
	}

	/**
	 * Constructs a response to a {@link Request} given the {@link Request} that caused it
	 * and information about the result of the execution.
	 *
	 * @param request       originating request
	 * @param type          result of execution
	 * @param message       result message
	 * @param timeRemaining time remaining in milliseconds until the effect completes
	 *                      or {@code 0} if the effect is not time-based
	 * @throws IllegalArgumentException May be thrown in various circumstances:
	 *                                  <ul>
	 *                                      <li>if the {@code request} is null</li>
	 *                                      <li>if the {@code type} is null</li>
	 *                                      <li>if the {@code timeRemaining} is negative</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	public Response(@NotNull Request request,
					@NotNull ResultType type,
					@Nullable String message,
					long timeRemaining) {
		this(ExceptionUtil.validateNotNull(request, "request").getId(),
				request.originatingSocket, PacketType.EFFECT_RESULT, type, message, timeRemaining);
	}

	/**
	 * Constructs a response to a {@link Request} from a {@link Builder}.
	 *
	 * @param builder {@link Response} builder
	 */
	@CheckReturnValue
	private Response(@NotNull Builder builder) {
		this(ExceptionUtil.validateNotNull(builder, "builder").id,
				builder.originatingSocket, builder.packetType, builder.type, builder.message, builder.timeRemaining);
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
		ExceptionUtil.validateNotNull(json, "json");
		return ByteAdapter.GSON.fromJson(json, Response.class);
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

	/**
	 * Determines if this {@link Response} marks the end to a series of responses to a
	 * {@link Request}.
	 *
	 * @return true if this response marks the end of a series of responses
	 */
	public boolean isTerminating() throws IllegalStateException {
		if (packetType != PacketType.EFFECT_RESULT)
			throw new IllegalStateException("This response is not an effect result");
		return type.isTerminating() || (type == ResultType.SUCCESS && timeRemaining == 0);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Response response = (Response) o;
		return id == response.id
				&& timeRemaining == response.timeRemaining
				&& packetType == response.packetType
				&& type == response.type
				&& Objects.equals(message, response.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(packetType, id, type, message, timeRemaining);
	}

	/**
	 * Determines if the {@link Request} that originated this {@link Response} is known.
	 * If not, {@link #send()} will throw an {@link IllegalStateException}.
	 *
	 * @return true if the request is known
	 */
	public boolean isOriginKnown() {
		return originatingSocket != null;
	}

	/**
	 * Sends this {@link Response} to the client or server that delivered the related {@link Request}.
	 *
	 * @throws IllegalStateException if {@link #isOriginKnown()} returns false
	 *                               (i.e. the response was created without a {@link Request})
	 */
	public void send() throws IllegalStateException {
		if (!isOriginKnown()) {
			throw new IllegalStateException("Response was constructed without a Request and thus cannot find where to be sent");
		}

		if (originatingSocket.isClosed() || !originatingSocket.isConnected() || originatingSocket.isOutputShutdown()) {
			return;
		}

		//object is never updated after assignment, so we can ignore this error:
		//noinspection SynchronizeOnNonFinalField
		synchronized (originatingSocket) {
			try {
				OutputStream output = originatingSocket.getOutputStream();
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
		SUCCESS(false),
		/**
		 * The effect failed to be applied. Will refund the purchaser.
		 */
		FAILURE(true),
		/**
		 * The requested effect is unusable and should not be requested again.
		 */
		UNAVAILABLE(true),
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 */
		RETRY(false),
		/**
		 * The timed effect has been paused and is now waiting.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		PAUSED(false, (byte) 0x06),
		/**
		 * The timed effect has been resumed and is counting down again.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		RESUMED(false, (byte) 0x07),
		/**
		 * The timed effect has finished.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		FINISHED(true, (byte) 0x08),
		/**
		 * Indicates that this Crowd Control server is not yet accepting requests.
		 * <p>
		 * This is an internal field used to indicate that the login process with a client has
		 * not yet completed. You should instead use {@link #FAILURE} to indicate a
		 * temporary failure or {@link #UNAVAILABLE} to indicate a permanent failure.
		 */
		NOT_READY(true, (byte) 0xFF);

		private static final Map<Byte, ResultType> BY_BYTE;

		static {
			Map<Byte, ResultType> map = new HashMap<>(values().length);
			for (ResultType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final boolean terminating;
		private final byte encodedByte;

		ResultType(boolean terminating, byte encodedByte) {
			this.terminating = terminating;
			this.encodedByte = encodedByte;
		}

		ResultType(boolean terminating) {
			this.terminating = terminating;
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

		/**
		 * Determines if this result type always marks the end to a series of {@link Response}s to a
		 * {@link Request}.
		 *
		 * @return true if this result type always marks the end of a series of {@link Response}s
		 */
		public boolean isTerminating() {
			return terminating;
		}
	}

	/**
	 * Mutable builder for the immutable {@link Response} class.
	 */
	public static class Builder implements Cloneable {
		private final int id;
		private final Socket originatingSocket;
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
		private Builder(@NotNull Response source) {
			this.id = source.getId();
			this.originatingSocket = source.originatingSocket;
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
			this.id = request.getId();
			this.originatingSocket = request.originatingSocket;
		}

		/**
		 * Creates a copy of the provided builder.
		 *
		 * @param builder builder to copy
		 */
		@CheckReturnValue
		private Builder(@NotNull Builder builder) {
			this.id = builder.id;
			this.originatingSocket = builder.originatingSocket;
			this.type = builder.type;
			this.message = builder.message;
			this.timeRemaining = builder.timeRemaining;
			this.packetType = builder.packetType;
			this.messageSet = builder.messageSet;
		}

		/**
		 * Manually creates a new builder with the given id and socket.
		 *
		 * @param id                id of the response
		 * @param originatingSocket socket that originated the request
		 */
		@CheckReturnValue
		Builder(int id, @Nullable Socket originatingSocket) {
			this.id = id;
			this.originatingSocket = originatingSocket;
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
		 * @throws IllegalArgumentException if timeRemaining is negative
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @see #timeRemaining(long, TimeUnit)
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
		 * @param timeRemaining time in the specified time unit
		 * @param timeUnit      time unit
		 * @return this builder
		 * @throws IllegalArgumentException if timeRemaining is negative
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		@NotNull
		@Contract("_, _ -> this")
		public Builder timeRemaining(long timeRemaining, @NotNull TimeUnit timeUnit) {
			return timeRemaining(timeUnit.toMillis(timeRemaining));
		}

		/**
		 * Sets the time left on the referenced effect.
		 *
		 * @param timeRemaining effect duration
		 * @return this builder
		 * @throws IllegalArgumentException if timeRemaining is negative
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Duration timeRemaining) {
			if (timeRemaining == null) {
				this.timeRemaining = 0;
				return this;
			}
			return timeRemaining(timeRemaining.toMillis());
		}

		/**
		 * Sets the time at which the referenced effect will end.
		 *
		 * @param endEffectAt time to end effect
		 * @return this builder
		 * @throws IllegalArgumentException if endEffectAt is in the past
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Temporal endEffectAt) throws IllegalArgumentException {
			if (endEffectAt == null) {
				timeRemaining = 0;
				return this;
			}
			return timeRemaining(ChronoUnit.MILLIS.between(LocalDateTime.now(), endEffectAt));
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

		// getters

		/**
		 * Gets the ID of the {@link Request} that prompted this {@link Response}.
		 *
		 * @return request ID
		 */
		public int id() {
			return id;
		}

		/**
		 * Gets the {@link Socket} of the {@link Request} that prompted this {@link Response}.
		 *
		 * @return originating socket
		 */
		@Nullable
		Socket originatingSocket() {
			return originatingSocket;
		}

		/**
		 * Gets the type of result being returned.
		 *
		 * @return result type
		 */
		@Nullable
		public ResultType type() {
			return type;
		}

		/**
		 * Gets the message describing or explaining the response.
		 *
		 * @return response message
		 */
		@Nullable
		public String message() {
			return message;
		}

		/**
		 * Gets the time left on the referenced effect in milliseconds.
		 *
		 * @return time in milliseconds
		 */
		public long timeRemaining() {
			return timeRemaining;
		}

		/**
		 * Gets the type of packet that this {@link Response} represents.
		 *
		 * @return packet type
		 */
		@Nullable
		public PacketType packetType() {
			return packetType;
		}

		// miscellaneous

		/**
		 * Builds a new {@link Response} object.
		 *
		 * @return new Response
		 */
		@NotNull
		@CheckReturnValue
		public Response build() {
			return new Response(this);
		}

		/**
		 * Builds this {@link Response} and then sends it to the client or server that delivered the related {@link Request}.
		 *
		 * @throws IllegalStateException if the response was created without a {@link Request}
		 */
		public void send() throws IllegalStateException {
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
			return new Builder(this);
		}
	}
}
