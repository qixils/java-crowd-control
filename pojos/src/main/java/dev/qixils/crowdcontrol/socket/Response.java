package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.qixils.crowdcontrol.util.StringUtils.repr;

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
	@Nullable
	private transient SocketManager originatingSocket;
	private int id = 0;
	@SerializedName("status")
	@Nullable
	private ResultType type;
	@Nullable
	private String message;
	@Nullable
	private Duration timeRemaining; // millis
	private String @Nullable [] ids;
	@Nullable
	private IdType idType;
	@Nullable
	private String method;
	@Nullable
	private Object @Nullable [] args;
	@Nullable
	private Map<@NotNull String, @Nullable Object> data;
	@Nullable
	private String eventType;
	@Nullable
	private Boolean internal;

	/**
	 * Instantiates an empty {@link Response}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	@SuppressWarnings("unused") // used by GSON
	Response() {
	}

	/**
	 * Constructs a response to a {@link Request} from a {@link Builder}.
	 *
	 * @param builder {@link Response} builder
	 */
	@CheckReturnValue
	Response(@NotNull Builder builder) {
		this.id = builder.id;
		if (this.id < 0)
			throw new IllegalArgumentException("ID cannot be negative");

		this.timeRemaining = builder.timeRemaining;
		if (this.timeRemaining != null && (this.timeRemaining.isNegative() || this.timeRemaining.isZero()))
			throw new IllegalArgumentException("timeRemaining must be positive or null");

		this.originatingSocket = builder.originatingSocket;
		this.method = builder.method;
		if (!builder.args.isEmpty())
			this.args = builder.args.toArray();
		if (!builder.data.isEmpty())
			this.data = builder.data;
		this.eventType = builder.eventType;
		this.internal = builder.internal;

		// validate packet type and result type
		this.packetType = ExceptionUtil.validateNotNullElse(builder.packetType, PacketType.EFFECT_RESULT);
		this.type = builder.type;
		if (this.packetType.hasResultType() && this.type == null)
			throw new IllegalArgumentException("type cannot be null if packetType requires a result type");
		if (!this.packetType.hasResultType() && this.type != null)
			throw new IllegalArgumentException("type cannot be non-null if packetType does not require a result type");
		//noinspection DataFlowIssue - type cannot be null per above
		if (this.packetType == PacketType.EFFECT_STATUS && !this.type.isStatus())
			throw new IllegalArgumentException("type must be a status if packetType is EFFECT_STATUS");
		if (this.packetType != PacketType.EFFECT_STATUS && this.type != null && this.type.isStatus())
			throw new IllegalArgumentException("type must not be a status if packetType is not EFFECT_STATUS");
		if (this.packetType == PacketType.EFFECT_STATUS && builder.ids.isEmpty())
			throw new IllegalArgumentException("ids cannot be empty if packetType is EFFECT_STATUS");

		if (this.packetType == PacketType.REMOTE_FUNCTION && this.method == null)
			throw new IllegalArgumentException("method cannot be null if packetType is REMOTE_FUNCTION");

		if (this.packetType == PacketType.GENERIC_EVENT && this.eventType == null)
			throw new IllegalArgumentException("eventType cannot be null if packetType is GENERIC_EVENT");

		// set message
		if (builder.message != null)
			this.message = builder.message;
		if (this.message == null && this.packetType.isMessageRequired())
			throw new IllegalArgumentException("message cannot be null if packetType requires a message");

		// set IDs
		this.ids = this.packetType != PacketType.EFFECT_STATUS ? null : builder.ids.toArray(new String[0]);
		this.idType = this.ids == null ? null : ExceptionUtil.validateNotNullElse(builder.idType, IdType.EFFECT);
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
	 * @param originatingSocket socket being terminated
	 * @param message           message describing the reason for termination
	 * @return a new Response object
	 * @throws IllegalArgumentException if the socket is null
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@CheckReturnValue
	@NotNull
	static Response ofDisconnectMessage(@NotNull SocketManager originatingSocket, @Nullable String message) {
		return new Response.Builder()
				.originatingSocket(originatingSocket)
				.packetType(PacketType.DISCONNECT)
				.message(ExceptionUtil.validateNotNullElse(message, "Disconnected"))
				.build();
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
		return new Response.Builder()
				.originatingSocket(ExceptionUtil.validateNotNull(request, "request").getOriginatingSocket())
				.packetType(PacketType.DISCONNECT)
				.message(ExceptionUtil.validateNotNullElse(message, "Disconnected"))
				.build();
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
	 * <p>
	 * This will return {@code null} when the {@link #getPacketType() packet type} is not
	 * {@link PacketType#EFFECT_RESULT EFFECT_RESULT}.
	 * However, this generally shouldn't be a concern unless you are directly working with library
	 * internals.
	 *
	 * @return effect result type or {@code null}
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@CheckReturnValue
	@UnknownNullability("non-null for regular use cases")
	@SuppressWarnings("NullabilityAnnotations")
	public ResultType getResultType() {
		return type;
	}

	/**
	 * Gets the type of packet represented by this response.
	 *
	 * @return packet type
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@NotNull
	@CheckReturnValue
	public PacketType getPacketType() {
		return packetType;
	}

	/**
	 * Gets the message that will be delivered along with the result.
	 * May be {@code null} if a {@link PacketType#isMessageRequired() message is not required}
	 * per {@link #getPacketType()}.
	 *
	 * @return result message
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@Nullable
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the time left until the referenced effect ends.
	 *
	 * @return effect duration
	 * @since 2.0.0
	 */
	@ApiStatus.AvailableSince("2.0.0")
	@CheckReturnValue
	@Nullable
	public Duration getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Gets the IDs receiving a status update.
	 *
	 * @return effect IDs
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	@CheckReturnValue
	public String @Nullable [] getIds() {
		if (ids == null) return null;
		return Arrays.copyOf(ids, ids.length);
	}

	/**
	 * Gets the type of effect receiving a status update.
	 *
	 * @return effect type
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	@CheckReturnValue
	@Nullable
	public IdType getIdType() {
		return idType;
	}

	/**
	 * Gets the name of the remote function to be executed.
	 * Null if the packet type is not {@link PacketType#REMOTE_FUNCTION REMOTE_FUNCTION}.
	 *
	 * @return remote function name
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@CheckReturnValue
	@Nullable
	public String getMethod() {
		return method;
	}

	/**
	 * Gets a copy of the arguments to be passed to the remote function.
	 * Null if the packet type is not {@link PacketType#REMOTE_FUNCTION REMOTE_FUNCTION}.
	 *
	 * @return remote function arguments
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@CheckReturnValue
	@Nullable
	public Object @Nullable [] getArguments() {
		if (args == null) return null;
		return Arrays.copyOf(args, args.length);
	}

	/**
	 * Gets a view of the event data.
	 * Null if the packet type is not {@link PacketType#GENERIC_EVENT GENERIC_EVENT}.
	 *
	 * @return event data
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	@CheckReturnValue
	@Nullable
	public Map<@NotNull String, @Nullable Object> getData() {
		if (data == null) return null;
		return Collections.unmodifiableMap(data);
	}

	/**
	 * Gets the name of the event being fired.
	 * Null if the packet type is not {@link PacketType#GENERIC_EVENT GENERIC_EVENT}.
	 *
	 * @return event name
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	@CheckReturnValue
	@Nullable
	public String getEventType() {
		return eventType;
	}

	/**
	 * Gets whether this packet is internal.
	 *
	 * @return true if this packet is internal, false if not, null if unknown or not applicable
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	@CheckReturnValue
	@Nullable
	public Boolean isInternal() {
		return internal;
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
		return (type != null && type.isTerminating()) || (type == ResultType.SUCCESS && timeRemaining == null);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
		Response response = (Response) o;
		return id == response.id
				&& Objects.equals(timeRemaining, response.timeRemaining)
				&& packetType == response.packetType
				&& type == response.type
				&& Objects.equals(message, response.message)
				&& Arrays.equals(ids, response.ids)
				&& Objects.equals(idType, response.idType)
				&& Objects.equals(method, response.method)
				&& Arrays.equals(args, response.args)
				&& Objects.equals(data, response.data)
				&& Objects.equals(eventType, response.eventType)
				&& Objects.equals(internal, response.internal)
				&& Objects.equals(originatingSocket, response.originatingSocket);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(packetType, originatingSocket, id, type, message, timeRemaining, idType, method, eventType, internal, data);
		result = 31 * result + Arrays.hashCode(ids);
		result = 31 * result + Arrays.hashCode(args);
		return result;
	}

	@Override
	public String toString() {
		return "Response{" +
				"packetType=" + packetType +
				", originatingSocket=" + originatingSocket +
				", id=" + id +
				", type=" + type +
				", message=" + repr(message) +
				", timeRemaining=" + timeRemaining +
				", ids=" + Arrays.toString(ids) +
				", idType=" + idType +
				", method=" + repr(method) +
				", args=" + Arrays.toString(args) +
				", data=" + data +
				", eventType=" + repr(eventType) +
				", internal=" + internal +
				'}';
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
		if (originatingSocket == null) {
			throw new IllegalStateException("Response was constructed without a Request and thus cannot find where to be sent");
		}

		if (originatingSocket.isClosed()) {
			return;
		}

		originatingSocket.write(this);
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
		EFFECT_RESULT(false, true), // 0
		/**
		 * The packet is updating the status of effects.
		 * This should be used with an {@link Builder#id(int) id} of 0.
		 *
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		EFFECT_STATUS(false, true), // 1
		/**
		 * The packet is a generic event.
		 *
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		GENERIC_EVENT(false, false, (byte) 0x10), // 16
		/**
		 * The packet is triggering a remote function to be run in the CS.
		 * This should be used with an {@link Builder#id(int) id} of 0 and should specify the function name to execute
		 * in the {@link Builder#method(String) method} field.
		 * This may optionally be used with the {@link Builder#addArguments(Object...) args} field.
		 *
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		REMOTE_FUNCTION(false, false, (byte) 0xD0), // 208
		/**
		 * <b>Internal value</b> used to prompt a connecting client for a password.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		LOGIN(false, false, (byte) 0xF0), // 240
		/**
		 * <b>Internal value</b> used to indicate a successful login.
		 *
		 * @since 3.1.0
		 */
		@ApiStatus.AvailableSince("3.1.0")
		@ApiStatus.Internal
		LOGIN_SUCCESS(false, false, (byte) 0xF1), // 241
		/**
		 * <b>Internal value</b> used to indicate that the socket is being disconnected.
		 *
		 * @since 3.1.0
		 */
		@ApiStatus.AvailableSince("3.1.0")
		@ApiStatus.Internal
		DISCONNECT(true, false, (byte) 0xFE), // 254
		/**
		 * <b>Internal value</b> used to reply to a keep alive packet.
		 *
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		KEEP_ALIVE(false, false, (byte) 0xFF); // 255

		private static final Map<Byte, PacketType> BY_BYTE;

		static {
			Map<Byte, PacketType> map = new HashMap<>(values().length);
			for (PacketType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final byte encodedByte;
		private final boolean isMessageRequired;
		private final boolean hasResultType;

		PacketType(boolean isMessageRequired, boolean hasResultType, byte encodedByte) {
			this.isMessageRequired = isMessageRequired;
			this.hasResultType = hasResultType;
			this.encodedByte = encodedByte;
		}

		PacketType(boolean isMessageRequired, boolean hasResultType) {
			this.isMessageRequired = isMessageRequired;
			this.hasResultType = hasResultType;
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

		/**
		 * Determines if this packet type requires an accompanying {@link ResultType} to be sent.
		 *
		 * @return true if a result type is required
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		@CheckReturnValue
		public boolean hasResultType() {
			return hasResultType;
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
		SUCCESS(false, false), // 0
		/**
		 * The effect failed to be applied. Will refund the purchaser.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		FAILURE(true, false), // 1
		/**
		 * The requested effect is unusable and should not be requested again.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		UNAVAILABLE(true, false), // 2
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 *
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		RETRY(false, false), // 3
		/**
		 * The timed effect has been paused and is now waiting.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		PAUSED(false, false, (byte) 0x06), // 6
		/**
		 * The timed effect has been resumed and is counting down again.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		RESUMED(false, false, (byte) 0x07), // 7
		/**
		 * The timed effect has finished.
		 *
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		FINISHED(true, false, (byte) 0x08), // 8
		/**
		 * Instructs the client to display this effect in its menu.
		 * <p>
		 * This type is not intended to be used as a response for an actual effect but rather sent to the client as
		 * necessary to update the status of an effect in the menu.
		 * This must be used in combination with {@link PacketType#EFFECT_STATUS}.
		 *
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		VISIBLE(true, true, (byte) 0x80), // 128
		/**
		 * Instructs the client to hide this effect in its menu.
		 * <p>
		 * This type is not intended to be used as a response for an actual effect but rather sent to the client as
		 * necessary to update the status of an effect in the menu.
		 * This must be used in combination with {@link PacketType#EFFECT_STATUS}.
		 *
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		NOT_VISIBLE(true, true, (byte) 0x81), // 129
		/**
		 * Instructs the client to make this effect in its menu selectable.
		 * <p>
		 * This type is not intended to be used as a response for an actual effect but rather sent to the client as
		 * necessary to update the status of an effect in the menu.
		 * This must be used in combination with {@link PacketType#EFFECT_STATUS}.
		 *
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		SELECTABLE(true, true, (byte) 0x82), // 130
		/**
		 * Instructs the client to make this effect in its menu unselectable.
		 * <p>
		 * This type is not intended to be used as a response for an actual effect but rather sent to the client as
		 * necessary to update the status of an effect in the menu.
		 * This must be used in combination with {@link PacketType#EFFECT_STATUS}.
		 *
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		NOT_SELECTABLE(true, true, (byte) 0x83), // 131
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
		NOT_READY(true, false, (byte) 0xFF); // 255

		private static final Map<Byte, ResultType> BY_BYTE;

		static {
			Map<Byte, ResultType> map = new HashMap<>(values().length);
			for (ResultType type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final boolean terminating;
		private final byte encodedByte;
		private final boolean isStatus;

		ResultType(boolean terminating, boolean isStatus, byte encodedByte) {
			this.terminating = terminating;
			this.isStatus = isStatus;
			this.encodedByte = encodedByte;
		}

		ResultType(boolean terminating, boolean isStatus) {
			this.terminating = terminating;
			this.isStatus = isStatus;
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

		/**
		 * Determines if this result type must be used in combination with {@link PacketType#EFFECT_STATUS}
		 * and an effect ID of 0.
		 *
		 * @return true if this result type is for an effect status packet
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		@CheckReturnValue
		public boolean isStatus() {
			return isStatus;
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
		private SocketManager originatingSocket;
		private ResultType type;
		private String message;
		private Duration timeRemaining;
		private PacketType packetType;
		private final List<String> ids = new ArrayList<>();
		private IdType idType;
		private String method;
		private final List<Object> args = new ArrayList<>();
		private final Map<String, Object> data = new HashMap<>();
		private String eventType;
		private Boolean internal;

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
			if (source.ids != null)
				Collections.addAll(this.ids, source.ids);
			this.idType = source.idType;
			this.method = source.method;
			if (source.args != null)
				Collections.addAll(this.args, source.args);
			if (source.data != null)
				this.data.putAll(source.data);
			this.eventType = source.eventType;
			this.internal = source.internal;
		}

		/**
		 * Creates a new builder representing the {@link Response} to a {@link Request}.
		 * <p>
		 * For internal use only; use {@link Request#buildResponse()} instead.
		 *
		 * @param request request to respond to
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@CheckReturnValue
		@ApiStatus.Internal
		protected Builder(@NotNull Request request) {
			this.id = request.getId();
			this.originatingSocket = request.getOriginatingSocket();
			if (request.getEffect() != null)
				this.ids.add(request.getEffect());
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
			this.ids.addAll(builder.ids);
			this.idType = builder.idType;
			this.method = builder.method;
			this.args.addAll(builder.args);
			this.data.putAll(builder.data);
			this.eventType = builder.eventType;
			this.internal = builder.internal;
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
		@ApiStatus.Internal
		@NotNull
		@Contract("_ -> this")
		public Builder originatingSocket(@Nullable SocketManager originatingSocket) {
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
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(long timeRemaining) throws IllegalArgumentException {
			if (timeRemaining == 0)
				this.timeRemaining = null;
			else if (timeRemaining < 0)
				throw new IllegalArgumentException("timeRemaining cannot be negative");
			else
				this.timeRemaining = Duration.ofMillis(timeRemaining);
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
			if (timeRemaining == 0)
				this.timeRemaining = null;
			else if (timeRemaining < 0)
				throw new IllegalArgumentException("timeRemaining cannot be negative");
			else
				this.timeRemaining = Duration.ofMillis(timeUnit.toMillis(timeRemaining));
			return this;
		}

		/**
		 * Sets the time left on the referenced effect.
		 *
		 * @param timeRemaining time in the specified temporal unit
		 * @param temporalUnit  temporal unit
		 * @return this builder
		 * @throws IllegalArgumentException if timeRemaining is negative
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_, _ -> this")
		public Builder timeRemaining(long timeRemaining, @NotNull TemporalUnit temporalUnit) throws IllegalArgumentException {
			if (timeRemaining == 0)
				this.timeRemaining = null;
			else if (timeRemaining < 0)
				throw new IllegalArgumentException("timeRemaining cannot be negative");
			else
				this.timeRemaining = Duration.of(timeRemaining, temporalUnit);
			return this;
		}

		/**
		 * Sets the time left on the referenced effect.
		 *
		 * @param timeRemaining effect duration
		 * @return this builder
		 * @throws IllegalArgumentException if timeRemaining is not positive
		 * @see dev.qixils.crowdcontrol.TimedEffect
		 * @since 2.0.0
		 */
		@ApiStatus.AvailableSince("2.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Duration timeRemaining) throws IllegalArgumentException {
			if (timeRemaining != null && (timeRemaining.isNegative() || timeRemaining.isZero()))
				throw new IllegalArgumentException("timeRemaining must be positive");
			this.timeRemaining = timeRemaining;
			return this;
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
				this.timeRemaining = null;
				return this;
			}
			return timeRemaining(Duration.between(Instant.now(), endEffectAt));
		}

		/**
		 * Sets the type of packet that this Response represents.
		 *
		 * @param packetType type of packet
		 * @return this builder
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@NotNull
		@Contract("_ -> this")
		public Builder packetType(@Nullable PacketType packetType) {
			this.packetType = packetType;
			return this;
		}

		/**
		 * Sets the effect that was referenced by the request.
		 *
		 * @param effect effect code
		 * @return this builder
		 * @since 3.5.2
		 * @deprecated Replaced by {@link #ids(String...)}.
		 */
		@ApiStatus.AvailableSince("3.5.2")
		@NotNull
		@Contract("_ -> this")
		public Builder effect(@Nullable String effect) {
			this.ids.clear();
			if (effect != null)
				this.ids.add(effect);
			return this;
		}

		/**
		 * Adds effects whose status is being reported on.
		 *
		 * @param effects effects codes
		 * @return this builder
		 * @since 3.7.0
		 */
		@ApiStatus.AvailableSince("3.7.0")
		@NotNull
		@Contract("_ -> this")
		public Builder ids(String @NotNull ... effects) {
			for (String effect : effects) {
				if (effect != null) {
					this.ids.add(effect);
				}
			}
			return this;
		}

		/**
		 * Adds effects whose status is being reported on.
		 *
		 * @param effects effects codes
		 * @return this builder
		 * @since 3.7.0
		 */
		@ApiStatus.AvailableSince("3.7.0")
		@NotNull
		@Contract("_ -> this")
		public Builder ids(@NotNull Iterable<String> effects) {
			for (String effect : effects) {
				if (effect != null) {
					this.ids.add(effect);
				}
			}
			return this;
		}

		/**
		 * Sets the type of ID which is being reported on.
		 *
		 * @param idType type of ID
		 * @return this builder
		 * @since 3.7.0
		 */
		@ApiStatus.AvailableSince("3.7.0")
		@NotNull
		@Contract("_ -> this")
		public Builder idType(@Nullable IdType idType) {
			this.idType = idType;
			return this;
		}

		/**
		 * Sets the name of the remote function to be called.
		 * To be used with {@link PacketType#REMOTE_FUNCTION}.
		 *
		 * @param method name of remote function
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder method(@Nullable String method) {
			this.method = method;
			return this;
		}

		/**
		 * Adds arguments to be passed to the remote function.
		 * To be used with {@link PacketType#REMOTE_FUNCTION}.
		 *
		 * @param arguments arguments to pass
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder addArguments(@Nullable Object @Nullable ... arguments) {
			if (arguments != null)
				Collections.addAll(this.args, arguments);
			return this;
		}

		/**
		 * Adds arguments to be passed to the remote function.
		 * To be used with {@link PacketType#REMOTE_FUNCTION}.
		 *
		 * @param arguments arguments to pass
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder addArguments(@Nullable Collection<@Nullable Object> arguments) {
			if (arguments != null)
				this.args.addAll(arguments);
			return this;
		}

		/**
		 * Adds data to be passed in this event.
		 * To be used with {@link PacketType#GENERIC_EVENT}.
		 *
		 * @param key   data key
		 * @param value data value
		 * @return this builder
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@Contract("_, _ -> this")
		public Builder putData(@NotNull String key, @Nullable Object value) {
			this.data.put(key, value);
			return this;
		}

		/**
		 * Sets the type of event being fired.
		 * To be used with {@link PacketType#GENERIC_EVENT}.
		 *
		 * @param eventType event type
		 * @return this builder
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@Contract("_ -> this")
		public Builder eventType(@Nullable String eventType) {
			this.eventType = eventType;
			return this;
		}

		/**
		 * Sets whether the packet is internal.
		 *
		 * @param internal whether the packet is internal
		 * @return this builder
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@Contract("_ -> this")
		public Builder internal(@Nullable Boolean internal) {
			this.internal = internal;
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
		@CheckReturnValue
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
		@CheckReturnValue
		public SocketManager originatingSocket() {
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
		@CheckReturnValue
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
		@CheckReturnValue
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
		@Nullable
		@CheckReturnValue
		public Duration timeRemaining() {
			return timeRemaining;
		}

		/**
		 * Gets the type of packet that this {@link Response} represents.
		 *
		 * @return packet type
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		public PacketType packetType() {
			return packetType;
		}

		/**
		 * Gets the effects whose status is being reported on.
		 *
		 * @return effect codes
		 * @since 3.7.0
		 */
		@ApiStatus.AvailableSince("3.7.0")
		@NotNull
		@CheckReturnValue
		public List<String> ids() {
			return ids;
		}

		/**
		 * Gets the type of ID whose status is being reported on.
		 *
		 * @return ID type
		 * @since 3.7.0
		 */
		@ApiStatus.AvailableSince("3.7.0")
		@Nullable
		@CheckReturnValue
		public IdType idType() {
			return idType;
		}

		/**
		 * Gets the name of the remote function to be called.
		 *
		 * @return name of remote function
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public String method() {
			return method;
		}

		/**
		 * Gets the arguments to be passed to the remote function.
		 *
		 * @return arguments to pass
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@CheckReturnValue
		public List<Object> arguments() {
			return args;
		}

		/**
		 * Gets the data to be passed in this event.
		 * To be used with {@link PacketType#GENERIC_EVENT}.
		 *
		 * @return data to pass
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@CheckReturnValue
		public Map<String, Object> data() {
			return data;
		}

		/**
		 * Gets the type of event being fired.
		 * To be used with {@link PacketType#GENERIC_EVENT}.
		 *
		 * @return event type
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@Nullable
		@CheckReturnValue
		public String eventType() {
			return eventType;
		}

		/**
		 * Gets whether the packet is internal.
		 *
		 * @return whether the packet is internal
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@Nullable
		@CheckReturnValue
		public Boolean internal() {
			return internal;
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
