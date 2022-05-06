package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * An outgoing packet to the Crowd Control TCP server carrying the result of executing
 * a {@link Request requested} effect.
 *
 * @see Request
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public class Response implements JsonObject {
	private static final @NotNull Logger logger = LoggerFactory.getLogger("CC-Response");
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
	 *                                      <li>if the {@code packetType} is {@link PacketType#EFFECT_RESULT}
	 *                                      and {@code type} is null</li>
	 *                                      <li>if the {@code packetType} is not {@link PacketType#EFFECT_RESULT}
	 *                                      and {@code type} is non-null</li>
	 *                                      <li>if the {@code message} is null,
	 *                                      {@code packetType} is not {@link PacketType#EFFECT_RESULT},
	 *                                      and {@link PacketType#isMessageRequired() packetType.isMessageRequired()} is true</li>
	 *                                  </ul>
	 */
	Response(int id,
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
		this.packetType = ExceptionUtil.validateNotNullElse(packetType, PacketType.EFFECT_RESULT);
		if (this.packetType == PacketType.EFFECT_RESULT && type == null)
			throw new IllegalArgumentException("type cannot be null if packetType is EFFECT_RESULT");
		else if (this.packetType != PacketType.EFFECT_RESULT && type != null)
			throw new IllegalArgumentException("type cannot be non-null if packetType is not EFFECT_RESULT");
		this.type = type;

		// set message
		if (message != null)
			this.message = message;
		else if (type != null)
			this.message = type.name();

		if (this.message == null && this.packetType.isMessageRequired())
			throw new IllegalArgumentException("message cannot be null if packetType requires a message");
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
	 *                                      <li>if the {@code message} is null
	 *                                      and {@link PacketType#isMessageRequired() packetType.isMessageRequired()} is true</li>
	 *                                  </ul>
	 */
	Response(int id,
			 @Nullable Socket originatingSocket,
			 @NotNull PacketType packetType,
			 @Nullable String message) throws IllegalArgumentException {
		if (packetType == PacketType.EFFECT_RESULT)
			throw new IllegalArgumentException("packetType cannot be EFFECT_RESULT in this constructor");
		this.id = id;
		if (this.id < 0)
			throw new IllegalArgumentException("ID cannot be negative");
		this.originatingSocket = originatingSocket;
		this.packetType = ExceptionUtil.validateNotNull(packetType, "packetType");
		if (packetType.isMessageRequired() && message == null)
			throw new IllegalArgumentException("message cannot be null if packetType requires a message");
		this.message = message;
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
	 *                                      <li>if the {@code message} is null
	 *                                      and {@link PacketType#isMessageRequired() packetType.isMessageRequired()} is true</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	Response(@NotNull Request request,
			 @NotNull PacketType packetType,
			 @Nullable String message) throws IllegalArgumentException {
		if (packetType == PacketType.EFFECT_RESULT)
			throw new IllegalArgumentException("packetType cannot be EFFECT_RESULT in this constructor");
		ExceptionUtil.validateNotNull(request, "request");
		this.id = request.getId();
		this.originatingSocket = request.originatingSocket;
		this.packetType = ExceptionUtil.validateNotNull(packetType, "packetType");
		this.type = null;
		if (message == null && packetType.isMessageRequired())
			throw new IllegalArgumentException("message cannot be null if packetType requires a message");
		this.message = message;
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
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public Response(@NotNull Request request,
					@NotNull ResultType type,
					@Nullable String message,
					long timeRemaining) throws IllegalArgumentException {
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
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@NotNull
	@CheckReturnValue
	public static Response fromJSON(@NotNull String json) throws JsonSyntaxException {
		ExceptionUtil.validateNotNull(json, "json");
		return ByteAdapter.GSON.fromJson(json, Response.class);
	}

	/**
	 * Creates a {@link Response} indicating that the socket connection is being terminated.
	 *
	 * @param id                ID of the request which caused this response
	 * @param originatingSocket socket being terminated
	 * @param message           message describing the reason for termination
	 * @return a new Response object
	 * @throws IllegalArgumentException if the socket is null
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@CheckReturnValue
	@NotNull
	static Response ofDisconnectMessage(int id, @NotNull Socket originatingSocket, @Nullable String message) {
		return new Response(id, originatingSocket, PacketType.DISCONNECT, ExceptionUtil.validateNotNullElse(message, "Disconnected"));
	}

	/**
	 * Creates a {@link Response} indicating that the socket connection is being terminated.
	 *
	 * @param originatingSocket socket being terminated
	 * @param message           message describing the reason for termination
	 * @return a new Response object
	 * @throws IllegalArgumentException if the socket is null
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@CheckReturnValue
	@NotNull
	static Response ofDisconnectMessage(@NotNull Socket originatingSocket, @Nullable String message) {
		return ofDisconnectMessage(0, originatingSocket, message);
	}

	/**
	 * Creates a {@link Response} indicating that the socket connection is being terminated.
	 *
	 * @param request request which caused this response
	 * @param message message describing the reason for termination
	 * @return a new Response object
	 * @throws IllegalArgumentException if the request is null
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@CheckReturnValue
	@NotNull
	static Response ofDisconnectMessage(@NotNull Request request, @Nullable String message) {
		if (request.originatingSocket == null)
			throw new IllegalArgumentException("request has no associated originating socket");
		return ofDisconnectMessage(request.getId(), request.originatingSocket, message);
	}

	/**
	 * Gets the ID of the outgoing packet. Corresponds to a unique transaction.
	 *
	 * @return packet ID
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@CheckReturnValue
	public int getId() {
		return id;
	}

	/**
	 * Gets the result of executing an effect.
	 *
	 * @return effect result
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@CheckReturnValue
	public @NotNull ResultType getResultType() {
		return type;
	}

	/**
	 * Gets the type of packet represented by this response.
	 * <p>
	 * Note: unless directly working with library internals, this will always be {@link PacketType#EFFECT_RESULT}.
	 *
	 * @return packet type
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@ApiStatus.Internal
	@NotNull
	@CheckReturnValue
	public PacketType getPacketType() {
		return packetType;
	}

	/**
	 * Gets the message that will be delivered along with the result.
	 *
	 * @return result message
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the milliseconds left until the referenced effect ends.
	 *
	 * @return effect duration in milliseconds
	 * @since 2.0.0
	 */
	@ApiStatus.AvailableSince("2.0.0")
	@CheckReturnValue
	public long getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Outputs this object as a JSON string for use in the server connection.
	 *
	 * @return JSON string
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@CheckReturnValue
	public String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}

	/**
	 * Creates a mutable {@link Builder} with a copy of the data in this {@link Response}.
	 *
	 * @return a new {@link Builder}
	 * @since 2.0.0
	 */
	@ApiStatus.AvailableSince("2.0.0")
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
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0") // TODO unit test this and the PacketType method
	public boolean isTerminating() throws IllegalStateException {
		if (packetType != PacketType.EFFECT_RESULT)
			throw new IllegalStateException("This response is not an effect result");
		return type.isTerminating() || (type == ResultType.SUCCESS && timeRemaining == 0);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
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
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isOriginKnown() {
		return originatingSocket != null;
	}

	/**
	 * Sends this {@link Response} to the client or server that delivered the related {@link Request}.
	 *
	 * @throws IllegalStateException if {@link #isOriginKnown()} returns false
	 *                               (i.e. the response was created without a {@link Request})
	 * @since 3.0.0
	 * @return whether the response was successfully sent
	 *         (false if an IOException occurred, true otherwise)
	 */
	@ApiStatus.AvailableSince("3.0.0")
	public boolean send() throws IllegalStateException {
		try {
			rawSend();
			return true;
		} catch (IOException exc) {
			logger.warn("Failed to write response to socket", exc);
			return false;
		}
	}

	void rawSend() throws IllegalStateException, IOException {
		if (!isOriginKnown()) {
			throw new IllegalStateException("Response was constructed without a Request and thus cannot find where to be sent");
		}

		if (originatingSocket.isClosed() || !originatingSocket.isConnected() || originatingSocket.isOutputShutdown()) {
			return;
		}

		//object is never updated after assignment, so we can ignore this error:
		//noinspection SynchronizeOnNonFinalField
		synchronized (originatingSocket) {
			OutputStream output = originatingSocket.getOutputStream();
			output.write(toJSON().getBytes(StandardCharsets.UTF_8));
			output.write(0x00);
			output.flush();
		}
	}

	/**
	 * Determines the type of packet being sent.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	public enum PacketType implements ByteObject {
		/**
		 * The packet is the result of executing an effect.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		EFFECT_RESULT(true),
		/**
		 * <b>Internal value</b> used to prompt a connecting client for a password.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		LOGIN(false, (byte) 0xF0),
		/**
		 * <b>Internal value</b> used to indicate a successful login.
		 *
		 * @since 3.1.0
		 */
		@ApiStatus.AvailableSince("3.1.0")
		@ApiStatus.Internal
		LOGIN_SUCCESS(false, (byte) 0xF1),
		/**
		 * <b>Internal value</b> used to indicate that the socket is being disconnected.
		 *
		 * @since 3.1.0
		 */
		@ApiStatus.AvailableSince("3.1.0")
		@ApiStatus.Internal
		DISCONNECT(true, (byte) 0xFE),
		/**
		 * <b>Internal value</b> used to reply to a keep alive packet.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		KEEP_ALIVE(false, (byte) 0xFF);

		private static final Map<Byte, PacketType> BY_BYTE;

		static {
			Map<Byte, PacketType> map = new HashMap<>(values().length);
			for (PacketType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final byte encodedByte;
		private final boolean isMessageRequired;

		PacketType(boolean isMessageRequired, byte encodedByte) {
			this.isMessageRequired = isMessageRequired;
			this.encodedByte = encodedByte;
		}

		PacketType(boolean isMessageRequired) {
			this.isMessageRequired = isMessageRequired;
			this.encodedByte = (byte) ordinal();
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 *
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		@CheckReturnValue
		public static @Nullable PacketType from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		@CheckReturnValue
		public byte getEncodedByte() {
			return encodedByte;
		}

		/**
		 * Determines if this packet type requires an accompanying message to be sent.
		 *
		 * @return true if a message is required
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@CheckReturnValue
		public boolean isMessageRequired() {
			return isMessageRequired;
		}
	}

	/**
	 * The result of processing an incoming packet.
	 *
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	public enum ResultType implements ByteObject {
		/**
		 * The effect was applied successfully.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		SUCCESS(false),
		/**
		 * The effect failed to be applied. Will refund the purchaser.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		FAILURE(true),
		/**
		 * The requested effect is unusable and should not be requested again.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		UNAVAILABLE(true),
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		RETRY(false),
		/**
		 * The timed effect has been paused and is now waiting.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		PAUSED(false, (byte) 0x06),
		/**
		 * The timed effect has been resumed and is counting down again.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		RESUMED(false, (byte) 0x07),
		/**
		 * The timed effect has finished.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		FINISHED(true, (byte) 0x08),
		/**
		 * Indicates that this Crowd Control server is not yet accepting requests.
		 * <p>
		 * This is an internal field used to indicate that the login process with a client has
		 * not yet completed. You should instead use {@link #FAILURE} to indicate a
		 * temporary failure or {@link #UNAVAILABLE} to indicate a permanent failure.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
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
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		@CheckReturnValue
		public static @Nullable ResultType from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		@CheckReturnValue
		public byte getEncodedByte() {
			return encodedByte;
		}

		/**
		 * Determines if this result type always marks the end to a series of {@link Response}s to a
		 * {@link Request}.
		 *
		 * @return true if this result type always marks the end of a series of {@link Response}s
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		public boolean isTerminating() {
			return terminating;
		}
	}

	/**
	 * Mutable builder for the immutable {@link Response} class.
	 *
	 * @since 2.0.0
	 */
	@ApiStatus.AvailableSince("2.0.0")
	public static class Builder implements Cloneable {
		// id & originatingSocket fields are final because the only way to construct this is via
		// a Request (i.e. third parties don't have access to the originating socket)
		private int id;
		private Socket originatingSocket;
		private ResultType type;
		private String message;
		private long timeRemaining;
		private PacketType packetType;

		/**
		 * Creates a new empty builder.
		 *
		 * @since 3.4.0
		 */
		@ApiStatus.AvailableSince("3.4.0")
		@CheckReturnValue
		public Builder() {
		}

		/**
		 * Creates a new builder using the data from a {@link Response}.
		 *
		 * @param source source for a new builder
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@CheckReturnValue
		protected Builder(@NotNull Response source) {
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
		 * @since 2.0.0
		 * @deprecated for removal in 3.5.0; replaced with {@link Request#buildResponse()}
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@CheckReturnValue
		@Deprecated
		@ApiStatus.ScheduledForRemoval(inVersion = "3.5.0")
		// to be made protected or package-private
		public Builder(@NotNull Request request) {
			this.id = request.getId();
			this.originatingSocket = request.originatingSocket;
		}

		/**
		 * Creates a copy of the provided builder.
		 *
		 * @param builder builder to copy
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		protected Builder(@NotNull Builder builder) {
			this.id = builder.id;
			this.originatingSocket = builder.originatingSocket;
			this.type = builder.type;
			this.message = builder.message;
			this.timeRemaining = builder.timeRemaining;
			this.packetType = builder.packetType;
		}

		/**
		 * Manually creates a new builder with the given id and socket.
		 *
		 * @param id                id of the response
		 * @param originatingSocket socket that originated the request
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		Builder(int id, @Nullable Socket originatingSocket) {
			this.id = id;
			this.originatingSocket = originatingSocket;
		}

		/**
		 * Sets the ID of the {@link Request} that prompted this {@link Response}.
		 *
		 * @param id id of the request
		 * @return this builder
		 * @since 3.4.0
		 */
		@ApiStatus.AvailableSince("3.4.0")
		@NotNull
		@Contract("_ -> this")
		public Builder id(int id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the {@link Socket} of the {@link Request} that prompted this {@link Response}.
		 *
		 * @param originatingSocket socket that originated the request
		 * @return this builder
		 * @since 3.4.0
		 */
		@ApiStatus.AvailableSince("3.4.0")
		@NotNull
		@Contract("_ -> this")
		Builder originatingSocket(@Nullable Socket originatingSocket) {
			this.originatingSocket = originatingSocket;
			return this;
		}

		/**
		 * Sets the type of result being returned.
		 *
		 * @param type result type
		 * @return this builder
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder type(@Nullable ResultType type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the message describing or explaining the response.
		 * <br>Useful for explaining why an effect failed to apply.
		 *
		 * @param message response message
		 * @return this builder
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder message(@Nullable String message) {
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
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(long timeRemaining) throws IllegalArgumentException {
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
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_, _ -> this")
		public Builder timeRemaining(long timeRemaining, @NotNull TimeUnit timeUnit) throws IllegalArgumentException {
			return timeRemaining(timeUnit.toMillis(timeRemaining));
		}

		/**
		 * Sets the time left on the referenced effect.
		 *
		 * @param timeRemaining effect duration
		 * @return this builder
		 * @throws IllegalArgumentException if timeRemaining is negative
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Duration timeRemaining) throws IllegalArgumentException {
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
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
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
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@NotNull
		@Contract("_ -> this")
		@ApiStatus.Internal
		public Builder packetType(@Nullable PacketType packetType) {
			this.packetType = packetType;
			return this;
		}

		// getters

		/**
		 * Gets the ID of the {@link Request} that prompted this {@link Response}.
		 *
		 * @return request ID
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		public int id() {
			return id;
		}

		/**
		 * Gets the {@link Socket} of the {@link Request} that prompted this {@link Response}.
		 *
		 * @return originating socket
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@ApiStatus.Internal
		@Nullable
		Socket originatingSocket() {
			return originatingSocket;
		}

		/**
		 * Gets the type of result being returned.
		 *
		 * @return result type
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		public ResultType type() {
			return type;
		}

		/**
		 * Gets the message describing or explaining the response.
		 *
		 * @return response message
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		public String message() {
			return message;
		}

		/**
		 * Gets the time left on the referenced effect in milliseconds.
		 *
		 * @return time in milliseconds
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		public long timeRemaining() {
			return timeRemaining;
		}

		/**
		 * Gets the type of packet that this {@link Response} represents.
		 * <p>
		 * Note: this is intended only for internal library use.
		 *
		 * @return packet type
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@ApiStatus.Internal
		public PacketType packetType() {
			return packetType;
		}

		// miscellaneous

		/**
		 * Builds a new {@link Response} object.
		 *
		 * @return new Response
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@CheckReturnValue
		public Response build() {
			return new Response(this);
		}

		/**
		 * Builds this {@link Response} and then sends it to the client or server that delivered the related {@link Request}.
		 *
		 * @throws IllegalStateException if the response was created without a {@link Request}
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		public void send() throws IllegalStateException {
			build().send();
		}

		/**
		 * Creates a new {@link Builder} object with the same parameters.
		 *
		 * @return cloned builder
		 * @since 2.1.0
		 */
		@ApiStatus.AvailableSince("2.1.0")
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		public @NotNull Builder clone() {
			return new Builder(this);
		}
	}
}
