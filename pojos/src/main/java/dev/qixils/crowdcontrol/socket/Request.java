package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 *
 * @see Response
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public class Request implements JsonObject {
	transient @Nullable Socket originatingSocket;
	private int id;
	private Type type;
	@SerializedName("code")
	private String effect;
	private String message;
	private String viewer;
	private Integer cost;
	private Target[] targets;

	/**
	 * Instantiates an empty {@link Request}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	@SuppressWarnings("unused") // used by GSON
	Request() {
	}

	/**
	 * Instantiates a {@link Request} with the given parameters.
	 *
	 * @param id      the ID of the request
	 * @param effect  the effect to be played
	 * @param message the message to be displayed
	 * @param viewer  the viewer who requested the effect
	 * @param cost    the cost of the effect
	 * @param type    the packet type to send
	 * @param targets the targets of the effect
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the given ID is negative</li>
	 *                                      <li>if the given packet type is null</li>
	 *                                      <li>if the given packet type is an {@link Type#isEffectType() effect type} and the effect or viewer is null</li>
	 *                                      <li>if the given packet type is not an {@link Type#isEffectType()} effect type} and the effect, viewer, cost, or targets is non-null</li>
	 *                                      <li>if the given packet type is {@link Type#LOGIN} and the message is null</li>
	 *                                  </ul>
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public Request(int id,
				   @NotNull Type type,
				   @Nullable String effect,
				   @Nullable String viewer,
				   @Nullable String message,
				   @Nullable Integer cost,
				   Target @Nullable [] targets) throws IllegalArgumentException {
		// validate request ID
		this.id = id;
		if (this.id < 0)
			throw new IllegalArgumentException("ID cannot be negative");

		// validate type & related arguments
		this.type = ExceptionUtil.validateNotNull(type, "type");
		if (type.isEffectType()) {
			if (effect == null)
				throw new IllegalArgumentException("effect cannot be null for effect packets");
			if (viewer == null)
				throw new IllegalArgumentException("viewer cannot be null for effect packets");
		} else {
			if (effect != null)
				throw new IllegalArgumentException("effect cannot be non-null for non-effect packets");
			if (viewer != null)
				throw new IllegalArgumentException("viewer cannot be non-null for non-effect packets");
			if (cost != null)
				throw new IllegalArgumentException("cost cannot be non-null for non-effect packets");
			if (targets != null)
				throw new IllegalArgumentException("targets cannot be non-null for non-effect packets");

			if (message == null && type == Type.LOGIN)
				throw new IllegalArgumentException("message (password) cannot be null for login packets");
		}

		// other arguments
		this.effect = effect == null ? null : effect.toLowerCase(Locale.ENGLISH);
		this.viewer = viewer == null ? null : viewer.toLowerCase(Locale.ENGLISH);
		this.message = message;
		this.cost = cost;

		// validate targets are not null
		if (targets != null) {
			for (Target target : targets) {
				if (target == null)
					throw new IllegalArgumentException("targets cannot contain null elements");
			}
		}
		this.targets = targets;
	}

	/**
	 * Instantiates a {@link Type#isEffectType() non-effect type} {@link Request} with the given parameters.
	 *
	 * @param id      the ID of the request
	 * @param type    the packet type to send
	 * @param message the message to be displayed
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the given ID is negative</li>
	 *                                      <li>if the given packet type is null</li>
	 *                                      <li>if the given packet type is an {@link Type#isEffectType() effect type}</li>
	 *                                      <li>if the given packet type is {@link Type#LOGIN} and the message is null</li>
	 *                                  </ul>
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public Request(int id, @NotNull Type type, @Nullable String message) throws IllegalArgumentException {
		if (id < 0)
			throw new IllegalArgumentException("ID cannot be negative");
		ExceptionUtil.validateNotNull(type, "type");
		if (type.isEffectType())
			throw new IllegalArgumentException("type cannot be an effect type");
		if (message == null && type == Type.LOGIN)
			throw new IllegalArgumentException("message (password) cannot be null for login packets");
		this.id = id;
		this.type = type;
		this.message = message;
	}

	/**
	 * Instantiates a {@link Request} object from a {@link Builder}.
	 *
	 * @param builder the {@link Builder} to use
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	private Request(Request.@NotNull Builder builder) {
		this(ExceptionUtil.validateNotNull(builder, "builder").id,
				builder.type, builder.effect, builder.viewer, builder.message, builder.cost, builder.targets);
		originatingSocket = builder.originatingSocket;
	}

	/**
	 * Creates a {@link Request} object from JSON.
	 *
	 * @param json input json data from the Crowd Control TCP server
	 * @return a new Request object
	 * @throws JsonSyntaxException the JSON failed to be parsed
	 * @since 1.0.0
	 */
	@NotNull
	@CheckReturnValue
	@ApiStatus.Internal
	@ApiStatus.AvailableSince("1.0.0")
	public static Request fromJSON(@NotNull String json) throws JsonSyntaxException {
		ExceptionUtil.validateNotNull(json, "json");
		return ByteAdapter.GSON.fromJson(json, Request.class);
	}

	/**
	 * Gets the ID of the incoming packet. Corresponds to a unique transaction.
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
	 * Gets the message from the incoming packet.
	 *
	 * @return message
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@Nullable
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the name of the effect to play.
	 *
	 * @return effect name
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@CheckReturnValue
	public String getEffect() {
		return effect;
	}

	/**
	 * Gets the name of the viewer who triggered the effect.
	 *
	 * @return viewer name
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@CheckReturnValue
	public String getViewer() {
		return viewer;
	}

	/**
	 * Gets the cost of the effect specified in this Request.
	 *
	 * @return effect cost
	 * @since 2.0.0
	 */
	@ApiStatus.AvailableSince("2.0.0")
	@Nullable
	@CheckReturnValue
	public Integer getCost() {
		return cost;
	}

	/**
	 * Gets the {@link Type Type} of the request.
	 *
	 * @return request type
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@CheckReturnValue
	public Type getType() {
		return type;
	}

	/**
	 * Gets the streamers being targeted by this effect.
	 * An empty array suggests that all players may be targeted.
	 *
	 * @return possibly empty array of {@link Target}
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public Target @NotNull [] getTargets() {
		if (targets == null)
			targets = new Target[0];
		return targets;
	}

	/**
	 * Determines if this Request is triggering an effect for all users.
	 *
	 * @return if the triggered effect is global
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public boolean isGlobal() {
		return targets == null || targets.length == 0;
	}

	/**
	 * Outputs this object as a JSON string.
	 *
	 * @return JSON string
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@ApiStatus.Internal
	@NotNull
	@CheckReturnValue
	public String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}

	/**
	 * Creates a mutable {@link Builder} with a copy of the data in this {@link Request}.
	 *
	 * @return a new {@link Builder}
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@NotNull
	@CheckReturnValue
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Creates a {@link Response.Builder Builder} for a {@link Response} to this request.
	 *
	 * @return new response builder
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public Response.@NotNull Builder buildResponse() {
		return new RequestResponseBuilder(this);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
		Request request = (Request) o;
		return id == request.id
				&& type == request.type
				&& Objects.equals(effect, request.effect)
				&& Objects.equals(message, request.message)
				&& Objects.equals(viewer, request.viewer)
				&& Objects.equals(cost, request.cost)
				&& Arrays.equals(getTargets(), request.getTargets());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, type, effect, message, viewer, cost);
		result = 31 * result + Arrays.hashCode(getTargets());
		return result;
	}

	/**
	 * The type of incoming packet.
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	public enum Type implements ByteObject {
		/**
		 * Indicates that you should simulate the starting of an effect (i.e. test if it's available)
		 * but should not actually start the effect.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		TEST(true),
		/**
		 * Indicates that you should start an effect, if available.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		START(true),
		/**
		 * Indicates that you should stop an effect.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		STOP(true),
		/**
		 * Indicates that a streamer is attempting to log in to the Crowd Control server.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		LOGIN(false, (byte) 0xF0),
		/**
		 * This packet's sole purpose is to establish that the connection with the
		 * Crowd Control server has not been dropped.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		KEEP_ALIVE(false, (byte) 0xFF);

		private static final @NotNull Map<Byte, Type> BY_BYTE;

		static {
			Map<Byte, Type> map = new HashMap<>(values().length);
			for (Type type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final boolean isStandard;
		private final byte encodedByte;

		Type(boolean isStandard, byte encodedByte) {
			this.isStandard = isStandard;
			this.encodedByte = encodedByte;
		}

		Type(boolean isStandard) {
			this.isStandard = isStandard;
			this.encodedByte = (byte) ordinal();
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 *
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 * @since 3.0.0
		 */
		@ApiStatus.Internal
		@ApiStatus.AvailableSince("3.0.0")
		@CheckReturnValue
		public static @Nullable Type from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		@ApiStatus.Internal
		@ApiStatus.AvailableSince("3.0.0")
		@CheckReturnValue
		public byte getEncodedByte() {
			return encodedByte;
		}

		/**
		 * Determines if this packet represents a standard effect request.
		 *
		 * @return if this packet represents a standard effect request
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		public boolean isEffectType() {
			return isStandard;
		}
	}

	/**
	 * A recipient of an effect.
	 * <p>
	 * This corresponds to a Twitch streamer connected to the Crowd Control server.
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	public static final class Target {
		private String id;
		private String name;
		private String avatar;

		/**
		 * Instantiates an empty {@link Target}.
		 * <p>
		 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
		 */
		@SuppressWarnings("unused") // used by GSON
		Target() {
		}

		/**
		 * Instantiates a {@link Target} with the given streamer information.
		 *
		 * @param id     streamer ID
		 * @param name   streamer name
		 * @param avatar streamer avatar
		 * @throws IllegalArgumentException if the ID is not positive or one of the other parameters is null
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		public Target(@NotNull String id, @NotNull String name, @NotNull String avatar) throws IllegalArgumentException {
			this.id = ExceptionUtil.validateNotNull(id, "id");
			this.name = ExceptionUtil.validateNotNull(name, "name");
			this.avatar = ExceptionUtil.validateNotNull(avatar, "avatar");
		}

		/**
		 * The recipient's ID.
		 * <p>
		 * Prior to 3.4.0, this method returned an integer representing a Twitch streamer ID.
		 * Now, it may return a string representing the ID of a streamer on Twitch or on a different
		 * platform.
		 *
		 * @return streamer ID
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@NotNull
		@CheckReturnValue
		public String getId() {
			return id;
		}

		/**
		 * The recipient's name on Twitch.
		 *
		 * @return Twitch username
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@NotNull
		@CheckReturnValue
		public String getName() {
			return name;
		}

		/**
		 * Gets the URL of the recipient's avatar on Twitch.
		 *
		 * @return Twitch avatar URL
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@NotNull
		@CheckReturnValue
		public String getAvatar() {
			return avatar;
		}

		@Override
		@CheckReturnValue
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Target target = (Target) o;
			return getId().equals(target.getId()) && getName().equals(target.getName()) && getAvatar().equals(target.getAvatar());
		}

		@Override
		@CheckReturnValue
		public int hashCode() {
			return Objects.hash(getId(), getName(), getAvatar());
		}
	}

	/**
	 * Mutable builder for the immutable {@link Request} class.
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@SuppressWarnings("DuplicatedCode") // not really fixable unless I added a getter interface (which is silly do to for this one constructor)
	public static class Builder implements Cloneable {
		private transient Socket originatingSocket;
		private int id = -1;
		private String effect;
		private String message;
		private String viewer;
		private @Nullable Integer cost;
		private Type type = Type.START;
		private Target @Nullable [] targets;

		/**
		 * Creates a new builder.
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		public Builder() {
		}

		/**
		 * Creates a new builder using the data from a {@link Request}.
		 *
		 * @param source source for a new builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		private Builder(@NotNull Request source) {
			this.id = source.id;
			this.originatingSocket = source.originatingSocket;
			this.effect = source.effect;
			this.message = source.message;
			this.viewer = source.viewer;
			this.cost = source.cost;
			this.type = source.type;
			this.targets = source.targets;
		}

		/**
		 * Creates a new builder from another builder.
		 *
		 * @param builder source for a new builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		private Builder(@NotNull Builder builder) {
			this.id = builder.id;
			this.originatingSocket = builder.originatingSocket;
			this.effect = builder.effect;
			this.message = builder.message;
			this.viewer = builder.viewer;
			this.cost = builder.cost;
			this.type = builder.type;
			this.targets = builder.targets;
		}

		// setters

		/**
		 * Sets the type of result being returned. Defaults to {@link Type#START}.
		 *
		 * @param type result type
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder type(@Nullable Type type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the message describing or explaining the request.
		 *
		 * @param message request message
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder message(@Nullable String message) {
			this.message = message;
			return this;
		}

		/**
		 * Sets the effect being requested.
		 *
		 * @param effect requested effect
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder effect(@Nullable String effect) {
			this.effect = effect;
			return this;
		}

		/**
		 * Sets the viewer requesting the effect.
		 *
		 * @param viewer viewer requesting the effect
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder viewer(@Nullable String viewer) {
			this.viewer = viewer;
			return this;
		}

		/**
		 * Sets the cost of the effect.
		 *
		 * @param cost cost of the effect
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder cost(@Nullable Integer cost) {
			this.cost = cost;
			return this;
		}

		/**
		 * Sets the targets of the effect.
		 *
		 * @param targets targets of the effect
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder targets(Target @Nullable ... targets) {
			this.targets = targets;
			return this;
		}

		/**
		 * Sets the ID of the request.
		 *
		 * @param id request ID
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		public Builder id(int id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the originating socket of the request.
		 *
		 * @param originatingSocket originating socket
		 * @return this builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@Contract("_ -> this")
		Builder originatingSocket(@Nullable Socket originatingSocket) {
			this.originatingSocket = originatingSocket;
			return this;
		}

		// getters

		/**
		 * Gets the ID of the request.
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
		 * Gets the effect being requested.
		 *
		 * @return requested effect
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		public String effect() {
			return effect;
		}

		/**
		 * Gets the message describing or explaining the request.
		 *
		 * @return request message
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		public String message() {
			return message;
		}

		/**
		 * Gets the viewer requesting the effect.
		 *
		 * @return viewer requesting the effect
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		public String viewer() {
			return viewer;
		}

		/**
		 * Gets the cost of the effect.
		 *
		 * @return cost of the effect
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		public Integer cost() {
			return cost;
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
		public Type type() {
			return type;
		}

		/**
		 * Gets the targets of the effect.
		 *
		 * @return targets of the effect
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@CheckReturnValue
		public Target @Nullable [] targets() {
			return targets;
		}

		/**
		 * Gets the originating socket of the request.
		 *
		 * @return originating socket
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@Nullable
		@CheckReturnValue
		Socket originatingSocket() {
			return originatingSocket;
		}

		// build

		/**
		 * Builds a new {@link Request} object.
		 *
		 * @return new Request
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@NotNull
		@CheckReturnValue
		public Request build() {
			return new Request(this);
		}

		/**
		 * Creates a new {@link Response.Builder} object with the same parameters.
		 *
		 * @return cloned builder
		 * @since 3.3.0
		 */
		@ApiStatus.AvailableSince("3.3.0")
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		public @NotNull Builder clone() {
			return new Builder(this);
		}
	}
}
