package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static dev.qixils.crowdcontrol.util.StringUtils.repr;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 *
 * @see Response
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public class Request implements JsonObject, Respondable {
	private static final Logger logger = LoggerFactory.getLogger(Request.class);
	private transient @Nullable Socket originatingSocket;
	private transient @Nullable Target source; // TODO: add to builder and add unit tests
	private int id;
	private Type type;
	@SerializedName("code")
	private String effect;
	private String message;
	private String viewer;
	@Nullable
	private Integer cost;
	private Target[] targets;
	@Nullable
	private Duration duration;
	private Object @Nullable [] parameters;

	/**
	 * Instantiates an empty {@link Request}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	@SuppressWarnings("unused") // used by GSON
	@ApiStatus.Internal
	Request() {
	}

	// TODO: (package)-private all constructors

	/**
	 * Instantiates a {@link Request} with the given parameters.
	 * <p>
	 * This constructor is marked as {@link ApiStatus.Experimental experimental} because it is frequently deprecated and
	 * eventually removed in new releases. Please use {@link Request.Builder} where possible instead.
	 *
	 * @param id        the ID of the request
	 * @param effect    the effect to be played
	 * @param message   the message to be displayed
	 * @param viewer    the viewer who requested the effect
	 * @param cost      the cost of the effect
	 * @param duration  the duration of the effect
	 * @param type      the packet type to send
	 * @param targets   the targets of the effect
	 * @param parameters the miscellaneous parameters of the effect
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the given ID is negative</li>
	 *                                      <li>if the given packet type is null</li>
	 *                                      <li>if the given packet type is an {@link Type#isEffectType() effect type} and the effect or viewer is null</li>
	 *                                      <li>if the given packet type is not an {@link Type#isEffectType()} effect type} and the effect, viewer, cost, or targets is non-null</li>
	 *                                      <li>if the given packet type is {@link Type#LOGIN} and the message is null</li>
	 *                                  </ul>
	 * @since 3.5.0
	 */
	@ApiStatus.AvailableSince("3.5.0")
	@ApiStatus.Experimental
	public Request(int id,
				   @NotNull Type type,
				   @Nullable String effect,
				   @Nullable String viewer,
				   @Nullable String message,
				   @Nullable Integer cost,
				   @Nullable Duration duration,
				   Target @Nullable [] targets,
				   Object @Nullable [] parameters) throws IllegalArgumentException {
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
		if (cost != null && cost < 0)
			throw new IllegalArgumentException("cost cannot be negative");
		this.cost = cost;
		if (duration != null && duration.isNegative())
			throw new IllegalArgumentException("duration cannot be negative");
		this.duration = duration;
		this.parameters = parameters;
		if (this.id == 0 && this.type.usesIncrementalIds() == TriState.TRUE)
			throw new IllegalArgumentException("ID cannot be 0 for effect packets");
		if (this.id != 0 && this.type.usesIncrementalIds() == TriState.FALSE)
			throw new IllegalArgumentException("ID must be 0 for non-effect packets");

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
				builder.type, builder.effect, builder.viewer, builder.message, builder.cost, builder.duration, builder.targets, builder.parameters);
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
		logger.debug("Incoming Packet: {}", json);
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
	 * May be null if {@link #getType()} is not an {@link Type#isEffectType() effect type}.
	 *
	 * @return effect name
	 * @since 1.0.0
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@Nullable
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
	 * Gets the streamer-specified duration of this effect.
	 * A null value suggests that the effect is not timed.
	 *
	 * @return duration if applicable, null otherwise
	 * @since 3.5.0
	 */
	@ApiStatus.AvailableSince("3.5.0")
	@Nullable
	@CheckReturnValue
	public Duration getDuration() {
		return duration;
	}

	/**
	 * Gets the miscellaneous parameters for this effect.
	 * This may be used by, for example, sliders defined in your CS file.
	 *
	 * @return array of objects if applicable, null otherwise
	 * @since 3.5.0
	 */
	@ApiStatus.AvailableSince("3.5.0")
	@CheckReturnValue
	public Object @Nullable [] getParameters() {
		return parameters;
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
	 * Gets the {@link Socket} that this {@link Request} originated from.
	 *
	 * @return originating socket
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@ApiStatus.Internal
	@Nullable
	public Socket getOriginatingSocket() {
		return originatingSocket;
	}

	/**
	 * Sets the {@link Socket} that this {@link Request} originated from.
	 *
	 * @param originatingSocket originating socket
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@ApiStatus.Internal
	public void setOriginatingSocket(@Nullable Socket originatingSocket) {
		this.originatingSocket = originatingSocket;
	}

	/**
	 * Gets the {@link Target streamer} that this {@link Request} originated from.
	 *
	 * @return originating streamer
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@Nullable
	public Target getSource() {
		return source;
	}

	/**
	 * Sets the {@link Target streamer} that this {@link Request} originated from.
	 *
	 * @param source originating streamer
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@ApiStatus.Internal
	public void setSource(@Nullable Target source) {
		this.source = source;
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
		return new Response.Builder(this);
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
				&& Arrays.equals(getTargets(), request.getTargets())
				&& Arrays.equals(getParameters(), request.getParameters())
				&& Objects.equals(duration, request.duration);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, type, effect, message, viewer, cost, duration);
		result = 31 * result + Arrays.hashCode(getTargets());
		result = 31 * result + Arrays.hashCode(getParameters());
		return result;
	}

	@Override
	public String toString() {
		return "Request{" +
				"originatingSocket=" + originatingSocket +
				", id=" + id +
				", type=" + type +
				", effect=" + repr(effect) +
				", message=" + repr(message) +
				", viewer=" + repr(viewer) +
				", cost=" + cost +
				", targets=" + Arrays.toString(targets) +
				", duration=" + duration +
				", parameters=" + Arrays.toString(parameters) +
				'}';
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
		TEST(TriState.TRUE),
		/**
		 * Indicates that you should start an effect, if available.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		START(TriState.TRUE),
		/**
		 * Indicates that you should stop an effect.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		STOP(TriState.TRUE),
		/**
		 * Identifies the connected player.
		 * @since 3.5.3
		 */
		@ApiStatus.AvailableSince("3.5.3")
		PLAYER_INFO(TriState.UNKNOWN, (byte) 0xE0),
		/**
		 * Indicates that a streamer is attempting to log in to the Crowd Control server.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		LOGIN(TriState.UNKNOWN, (byte) 0xF0),
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
		KEEP_ALIVE(TriState.FALSE, (byte) 0xFF);

		private static final @NotNull Map<Byte, Type> BY_BYTE;

		static {
			Map<Byte, Type> map = new HashMap<>(values().length);
			for (Type type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final @NotNull TriState isStandard;
		private final byte encodedByte;

		Type(@NotNull TriState isStandard, byte encodedByte) {
			this.isStandard = isStandard;
			this.encodedByte = encodedByte;
		}

		Type(@NotNull TriState isStandard) {
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
			return isStandard == TriState.TRUE;
		}

		/**
		 * Determines if this packet is expected to use incremental IDs.
		 *
		 * @return if this packet is expected to use incremental IDs
		 * @since 3.5.3
		 */
		@ApiStatus.AvailableSince("3.5.3")
		@NotNull
		public TriState usesIncrementalIds() {
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
		private @Nullable String id;
		private @Nullable String name;
		private @Nullable String login;
		private @Nullable String avatar;
		// private ServiceType service; currently unimplemented

		/**
		 * Instantiates an empty {@link Target}.
		 * <p>
		 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
		 */
		@SuppressWarnings("unused") // used by GSON
		Target() {
		}

		private Target(@NotNull Builder builder) {
			this.id = builder.id;
			this.name = builder.name;
			this.login = builder.login;
			this.avatar = builder.avatar;
		}

		/**
		 * The recipient's ID.
		 * <p>
		 * Prior to 3.4.0, this method returned an integer representing a Twitch streamer ID.
		 * Now, it may return a string representing the ID of a streamer on Twitch or on a different
		 * platform. It may also return null if the ID is unknown.
		 *
		 * @return streamer ID
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@Nullable
		@CheckReturnValue
		public String getId() {
			return id;
		}

		/**
		 * The recipient's display name.
		 * May be null if the name is unknown.
		 *
		 * @return display name
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@Nullable
		@CheckReturnValue
		public String getName() {
			return name;
		}

		/**
		 * The recipient's username.
		 * May be null if the username is unknown.
		 *
		 * @return username
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		@Nullable
		@CheckReturnValue
		public String getLogin() {
			return login;
		}

		/**
		 * Gets the URL of the recipient's avatar.
		 * May be null if the avatar is unknown.
		 *
		 * @return avatar URL
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@Nullable
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
			return Objects.equals(getId(), target.getId())
					&& Objects.equals(getName(), target.getName())
					&& Objects.equals(getLogin(), target.getLogin())
					&& Objects.equals(getAvatar(), target.getAvatar());
		}

		@Override
		@CheckReturnValue
		public int hashCode() {
			return Objects.hash(getId(), getName(), getLogin(), getAvatar());
		}

		@Override
		public String toString() {
			return "Target{" +
					"id=" + repr(id) +
					", name=" + repr(name) +
					", login=" + repr(login) +
					", avatar=" + repr(avatar) +
					'}';
		}

		/**
		 * Creates a mutable {@link Builder} with a copy of the data in this {@link Target}.
		 *
		 * @return a new {@link Builder}
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		@NotNull
		@CheckReturnValue
		public Builder toBuilder() {
			return new Builder(this);
		}

		/**
		 * Mutable builder for the immutable {@link Target} class.
		 * @since 3.5.2
		 */
		@ApiStatus.AvailableSince("3.5.2")
		public static final class Builder implements Cloneable {
			private @Nullable String id;
			private @Nullable String name;
			private @Nullable String login;
			private @Nullable String avatar;
			// private ServiceType service; currently unimplemented

			// constructors //

			/**
			 * Creates a new builder.
			 *
			 * @since 3.5.2
			 */
			public Builder() {
			}

			private Builder(@NotNull Target target) {
				this.id = target.id;
				this.name = target.name;
				this.login = target.login;
				this.avatar = target.avatar;
			}

			private Builder(@NotNull Builder builder) {
				this.id = builder.id;
				this.name = builder.name;
				this.login = builder.login;
				this.avatar = builder.avatar;
			}

			// setters //

			/**
			 * Sets the recipient's ID.
			 *
			 * @param id streamer ID
			 * @return this builder
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@NotNull
			@Contract("_ -> this")
			@CheckReturnValue
			public Builder id(@Nullable String id) {
				this.id = id;
				return this;
			}

			/**
			 * Sets the recipient's display name.
			 *
			 * @param name display name
			 * @return this builder
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@NotNull
			@Contract("_ -> this")
			@CheckReturnValue
			public Builder name(@Nullable String name) {
				this.name = name;
				return this;
			}

			/**
			 * Sets the recipient's username.
			 *
			 * @param login username
			 * @return this builder
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@NotNull
			@Contract("_ -> this")
			@CheckReturnValue
			public Builder login(@Nullable String login) {
				this.login = login;
				return this;
			}

			/**
			 * Sets the URL of the recipient's avatar.
			 *
			 * @param avatar avatar URL
			 * @return this builder
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@NotNull
			@Contract("_ -> this")
			@CheckReturnValue
			public Builder avatar(@Nullable String avatar) {
				this.avatar = avatar;
				return this;
			}

			// getters //

			/**
			 * Gets the recipient's ID.
			 *
			 * @return streamer ID
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@Nullable
			@CheckReturnValue
			public String id() {
				return id;
			}

			/**
			 * Gets the recipient's display name.
			 *
			 * @return display name
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@Nullable
			@CheckReturnValue
			public String name() {
				return name;
			}

			/**
			 * Gets the recipient's username.
			 *
			 * @return username
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@Nullable
			@CheckReturnValue
			public String login() {
				return login;
			}

			/**
			 * Gets the URL of the recipient's avatar.
			 *
			 * @return avatar URL
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@Nullable
			@CheckReturnValue
			public String avatar() {
				return avatar;
			}

			// misc

			/**
			 * Creates a new builder object with the same parameters.
			 *
			 * @return cloned builder
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@SuppressWarnings("MethodDoesntCallSuperMethod")
			@Override
			public @NotNull Builder clone() {
				return new Builder(this);
			}

			/**
			 * Builds a new {@link Target} object.
			 *
			 * @return new Request
			 * @since 3.5.2
			 */
			@ApiStatus.AvailableSince("3.5.2")
			@NotNull
			@CheckReturnValue
			public Target build() {
				return new Target(this);
			}
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
		private int id = 0;
		private String effect;
		private String message;
		private String viewer;
		private @Nullable Integer cost;
		private Type type = Type.START;
		private Target @Nullable [] targets;
		private @Nullable Duration duration;
		private Object @Nullable [] parameters;

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
			this.duration = source.duration;
			this.parameters = source.parameters;
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
			this.duration = builder.duration;
			this.parameters = builder.parameters;
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
		@ApiStatus.Internal
		@NotNull
		@Contract("_ -> this")
		public Builder originatingSocket(@Nullable Socket originatingSocket) {
			this.originatingSocket = originatingSocket;
			return this;
		}

		/**
		 * Sets the duration of the effect.
		 *
		 * @param duration duration of the effect
		 * @return this builder
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_ -> this")
		public Builder duration(@Nullable Duration duration) {
			this.duration = duration;
			return this;
		}

		/**
		 * Sets the parameters of the effect.
		 *
		 * @param parameters parameters of the effect
		 * @return this builder
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_ -> this")
		public Builder parameters(Object @Nullable ... parameters) {
			this.parameters = parameters;
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
		@ApiStatus.Internal
		@Nullable
		@CheckReturnValue
		public Socket originatingSocket() {
			return originatingSocket;
		}

		/**
		 * Gets the duration of the effect.
		 *
		 * @return duration of the effect
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@Nullable
		@CheckReturnValue
		public Duration duration() {
			return duration;
		}

		/**
		 * Gets the parameters of the effect.
		 *
		 * @return parameters of the effect
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@CheckReturnValue
		public Object @Nullable [] parameters() {
			return parameters;
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
		 * Creates a new builder object with the same parameters.
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
