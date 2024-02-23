package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.util.PostProcessable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;

import static dev.qixils.crowdcontrol.util.StringUtils.repr;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 *
 * @see Response
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public class Request implements JsonObject, Respondable {
	private static final Logger logger = LoggerFactory.getLogger("CrowdControl/Request");
	private transient @Nullable SocketManager originatingSocket;
	private transient @Nullable Source source;
	private int id;
	private Type type;
	@SerializedName("code")
	private String effect;
	private String message;
	private String viewer;
	// TODO: private Target[] viewers;
	@Nullable
	private Integer cost;
	private Target[] targets;
	@Nullable
	private Duration duration;
	//private Object @Nullable [] parameters;
	@Nullable
	private Object value;
	@Nullable
	private Integer quantity;
	@Nullable
	private String login;
	@Nullable
	private String password;
	@Nullable
	private Target player;

	/**
	 * Instantiates an empty {@link Request}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	@SuppressWarnings("unused") // used by GSON
	@ApiStatus.Internal
	Request() {
	}

	/**
	 * Instantiates a {@link Request} object from a {@link Builder}.
	 *
	 * @param builder the {@link Builder} to use
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the given ID is negative</li>
	 *                                      <li>if the given packet type is null</li>
	 *                                      <li>if the given packet type is an {@link Type#isEffectType() effect type} and the effect or viewer is null</li>
	 *                                      <li>if the given packet type is not an {@link Type#isEffectType()} effect type} and the effect, viewer, cost, or targets is non-null</li>
	 *                                      <li>if the given packet type is {@link Type#LOGIN} and the password is null</li>
	 *                                  </ul>
	 */
	private Request(Request.@NotNull Builder builder) throws IllegalArgumentException {
		// validate request ID
		this.id = builder.id;

		// validate type & related arguments
		this.type = ExceptionUtil.validateNotNull(builder.type, "type");
        if (!this.type.isEffectType()) {
            if (builder.effect != null)
                throw new IllegalArgumentException("effect cannot be non-null for non-effect packets");
            if (builder.viewer != null)
                throw new IllegalArgumentException("viewer cannot be non-null for non-effect packets");
            if (builder.cost != null)
                throw new IllegalArgumentException("cost cannot be non-null for non-effect packets");
            if (builder.targets != null)
                throw new IllegalArgumentException("targets cannot be non-null for non-effect packets");
            if (builder.quantity != null)
                throw new IllegalArgumentException("quantity cannot be non-null for non-effect packets");

            if (builder.password == null && this.type == Type.LOGIN)
                throw new IllegalArgumentException("password cannot be null for login packets");

            if (builder.player == null && this.type == Type.PLAYER_INFO)
                throw new IllegalArgumentException("player cannot be null for player info packets");
        }

        // other arguments
		this.effect = builder.effect == null ? null : builder.effect.toLowerCase(Locale.ENGLISH);
		this.viewer = builder.viewer;
		this.message = builder.message;
		if (builder.cost != null && builder.cost < 0)
			throw new IllegalArgumentException("cost cannot be negative");
		this.cost = builder.cost;
		if (builder.duration != null && builder.duration.isNegative())
			throw new IllegalArgumentException("duration cannot be negative");
		this.duration = builder.duration;
//		this.parameters = builder.parameters;
		if (this.type != Type.REMOTE_FUNCTION_RESULT && builder.value != null)
			throw new IllegalArgumentException("value cannot be non-null for non-remote function result packets");
		this.value = builder.value;
		this.quantity = builder.quantity;
		this.source = builder.source;
		this.originatingSocket = builder.originatingSocket;
		this.login = builder.login;
		this.password = builder.password;
		this.player = builder.player;

		// validate targets are not null
		if (builder.targets != null) {
			for (Target target : builder.targets) {
				if (target == null)
					throw new IllegalArgumentException("targets cannot contain null elements");
			}
		}
		this.targets = builder.targets;
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
//	@ApiStatus.AvailableSince("3.5.0")
//	@CheckReturnValue
//	public Object @Nullable [] getParameters() {
//		return parameters;
//	}

	/**
	 * Gets the value returned by the remote function.
	 * Applicable only when {@link #getType()} is {@link Type#REMOTE_FUNCTION_RESULT}.
	 *
	 * @return the value returned by the remote function
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@CheckReturnValue
	@Nullable
	public Object getValue() {
		return value;
	}

	/**
	 * Gets the quantity of the item to be added or removed.
	 * May be null for non-effect requests or for effects that do not involve items.
	 *
	 * @return the quantity of the item to be added or removed
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@CheckReturnValue
	@Nullable
	public Integer getQuantity() {
		return quantity;
	}

	/**
	 * Gets the quantity of the item to be added or removed.
	 * If null or below 1, returns 1.
	 *
	 * @return the quantity of the item to be added or removed
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@CheckReturnValue
	public int getQuantityOrDefault() {
		if (quantity == null) return 1;
		return Math.max(1, quantity);
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
	 * Gets the name or ID of the streamer in the game.
	 * Null when {@link #getType()} is not {@link Type#LOGIN}.
	 *
	 * @return streamer name or ID
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	@Nullable
	@CheckReturnValue
	public String getLogin() {
		return login;
	}

	/**
	 * Gets the submitted password.
	 * Non-null when {@link #getType()} is {@link Type#LOGIN}.
	 *
	 * @return password
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	@Nullable
	@CheckReturnValue
	public String getPassword() {
		return password;
	}

	/**
	 * If this is a {@link Type#PLAYER_INFO} packet, gets the player's info.
	 *
	 * @return player info
	 * @since 3.6.2
	 */
	@ApiStatus.AvailableSince("3.6.2")
	@Nullable
	@CheckReturnValue
	public Target getPlayer() {
		return player;
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
	public SocketManager getOriginatingSocket() {
		return originatingSocket;
	}

	/**
	 * Sets the {@link Socket} that this {@link Request} originated from.
	 * <p>
	 * This method is for internal use only, see {@link Builder#originatingSocket(SocketManager)} if you need to set this value.
	 *
	 * @param originatingSocket originating socket
	 * @since 3.6.0
	 */
	@ApiStatus.Internal
	public void setOriginatingSocket(@Nullable SocketManager originatingSocket) {
		this.originatingSocket = originatingSocket;
	}

	/**
	 * Gets the {@link Source streamer} that this {@link Request} originated from.
	 *
	 * @return originating streamer
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@Nullable
	public Source getSource() {
		return source;
	}

	/**
	 * Sets the {@link Source streamer} that this {@link Request} originated from.
	 * <p>
	 * This method is for internal use only, see {@link Builder#source(Source)} if you need to set this value.
	 *
	 * @param source originating streamer
	 * @since 3.6.0
	 */
	@ApiStatus.Internal
	public void setSource(@Nullable Source source) {
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
//				&& Arrays.equals(getParameters(), request.getParameters())
				&& Objects.equals(duration, request.duration)
//				&& Objects.equals(originatingSocket, request.originatingSocket)
				&& Objects.equals(source, request.source)
				&& Objects.equals(value, request.value)
				&& Objects.equals(login, request.login)
				&& Objects.equals(password, request.password)
				&& Objects.equals(player, request.player);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, type, effect, message, viewer, cost, duration, source, value, login, password, player);
		result = 31 * result + Arrays.hashCode(getTargets());
//		result = 31 * result + Arrays.hashCode(getParameters());
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
//				", parameters=" + Arrays.toString(parameters) +
				", source=" + source +
				", value=" + value +
				", login=" + repr(login) +
				", password=" + repr(password) +
				", player=" + player +
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
		TEST(TriState.TRUE), // 0
		/**
		 * Indicates that you should start an effect, if available.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		START(TriState.TRUE), // 1
		/**
		 * Indicates that you should stop an effect.
		 * @since 1.0.0
		 */
		@ApiStatus.AvailableSince("1.0.0")
		STOP(TriState.TRUE), // 2
		/**
		 * Indicates the result of a {@link Response.PacketType#REMOTE_FUNCTION}.
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		REMOTE_FUNCTION_RESULT(TriState.UNKNOWN, (byte) 0xD0), // 208
		/**
		 * Identifies the connected player.
		 * @since 3.5.3
		 */
		@ApiStatus.AvailableSince("3.5.3")
		PLAYER_INFO(TriState.UNKNOWN, (byte) 0xE0), // 224
		/**
		 * Indicates that a streamer is attempting to log in to the Crowd Control server.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 * @since 3.0.0
		 */
		@ApiStatus.AvailableSince("3.0.0")
		@ApiStatus.Internal
		LOGIN(TriState.UNKNOWN, (byte) 0xF0), // 240
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
		KEEP_ALIVE(TriState.FALSE, (byte) 0xFF); // 255

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
	 * This corresponds to a streamer connected to the Crowd Control server.
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	public static final class Target implements PostProcessable {
		@SerializedName(value = "id", alternate = {"originID"})
		private @Nullable String id;
		private @Nullable String name;
		private @Nullable String login;
		@SerializedName(value = "avatar", alternate = {"image"})
		private @Nullable String avatar;
		@SerializedName(value = "service", alternate = {"profile"})
		private @Nullable String service;
		private @Nullable String ccUID;

		/**
		 * Instantiates an empty {@link Target}.
		 * <p>
		 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
		 */
		@SuppressWarnings("unused") // used by GSON
		Target() {
		}

		@Override
		@ApiStatus.Internal
		public void postProcess() {
			if (id != null && service != null) {
				String[] split = id.split("_", 2);
				if (split.length == 2 && split[0].equalsIgnoreCase(service))
					id = split[1];
			}
		}

		private Target(@NotNull Builder builder) {
			this.id = builder.id;
			this.name = builder.name;
			this.login = builder.login;
			this.avatar = builder.avatar;
			this.service = builder.service;
			this.ccUID = builder.ccUID;
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

		/**
		 * Gets the service that the streamer is streaming on.
		 *
		 * @return service
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public String getService() {
			return service;
		}

		/**
		 * Gets the Crowd Control user ID of the recipient.
		 *
		 * @return Crowd Control user ID
		 * @since 3.6.2
		 */
		@ApiStatus.AvailableSince("3.6.2")
		@Nullable
		@CheckReturnValue
		public String getCCUID() {
			return ccUID;
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
					&& Objects.equals(getAvatar(), target.getAvatar())
					&& Objects.equals(getService(), target.getService())
					&& Objects.equals(getCCUID(), target.getCCUID());
		}

		/**
		 * Determines if the provided object is roughly equal to this {@link Target}.
		 * An object is roughly equal if its ID is non-null and equal to this {@link Target}'s, or if they are equal via
		 * {@link #equals(Object)}.
		 *
		 * @param o object to compare
		 * @return if the provided object is roughly equal to this {@link Target}
		 * @since 3.6.2
		 */
		@ApiStatus.AvailableSince("3.6.2")
		@CheckReturnValue
		public boolean equalsRoughly(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Target target = (Target) o;
			return (getId() != null && getId().equals(target.getId()))
					|| (getCCUID() != null && getCCUID().equals(target.getCCUID()))
					|| equals(o);
		}

		@Override
		@CheckReturnValue
		public int hashCode() {
			return Objects.hash(getId(), getName(), getLogin(), getAvatar(), getService(), getCCUID());
		}

		@Override
		public String toString() {
			return "Target{" +
					"id=" + repr(id) +
					", name=" + repr(name) +
					", login=" + repr(login) +
					", avatar=" + repr(avatar) +
					", service=" + repr(service) +
					", ccUID=" + repr(ccUID) +
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
			private @Nullable String service;
			private @Nullable String ccUID;

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
				this.service = target.service;
				this.ccUID = target.ccUID;
			}

			private Builder(@NotNull Builder builder) {
				this.id = builder.id;
				this.name = builder.name;
				this.login = builder.login;
				this.avatar = builder.avatar;
				this.service = builder.service;
				this.ccUID = builder.ccUID;
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
			public Builder avatar(@Nullable String avatar) {
				this.avatar = avatar;
				return this;
			}

			/**
			 * Sets the service that the streamer is streaming on.
			 *
			 * @param service service
			 * @return this builder
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@NotNull
			@Contract("_ -> this")
			public Builder service(@Nullable String service) {
				this.service = service;
				return this;
			}

			/**
			 * Sets the Crowd Control user ID of the recipient.
			 *
			 * @param ccUID Crowd Control user ID
			 * @return this builder
			 * @since 3.6.2
			 */
			@ApiStatus.AvailableSince("3.6.2")
			@NotNull
			@Contract("_ -> this")
			public Builder ccUID(@Nullable String ccUID) {
				this.ccUID = ccUID;
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

			/**
			 * Gets the service that the streamer is streaming on.
			 *
			 * @return service
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@Nullable
			@CheckReturnValue
			public String service() {
				return service;
			}

			/**
			 * Gets the Crowd Control user ID of the recipient.
			 *
			 * @return Crowd Control user ID
			 * @since 3.6.2
			 */
			@ApiStatus.AvailableSince("3.6.2")
			@Nullable
			@CheckReturnValue
			public String ccUID() {
				return ccUID;
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
	 * The source of an effect.
	 * <p>
	 * This corresponds to a streamer connected to the Crowd Control server.
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	public static final class Source {
		private final @Nullable Target target;
		private final @Nullable InetAddress ip;
		private final @Nullable String login;

		private Source(@NotNull Builder builder) {
			this.target = builder.target;
			this.ip = builder.ip;
			this.login = builder.login;
		}

		/**
		 * The identity of this source.
		 *
		 * @return the identity of this source
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public Target target() {
			return target;
		}

		/**
		 * The IP address of this source.
		 *
		 * @return the IP address of this source
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public InetAddress ip() {
			return ip;
		}

		/**
		 * The name or ID of the streamer in the game.
		 *
		 * @return the name or ID of the streamer in the game
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@Nullable
		@CheckReturnValue
		public String login() {
			return login;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Source source = (Source) o;
			return Objects.equals(target, source.target) && Objects.equals(ip, source.ip);
		}

		@Override
		public int hashCode() {
			return Objects.hash(target, ip);
		}

		@Override
		public String toString() {
			return "Source{" +
					"target=" + target +
					", ip=" + ip +
					", login=" + repr(login) +
					'}';
		}

		/**
		 * Creates a builder with the same parameters as this object.
		 *
		 * @return a builder with the same parameters as this object
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@CheckReturnValue
		public Builder toBuilder() {
			return new Builder(this);
		}

		/**
		 * Mutable builder for the immutable {@link Source} class.
		 *
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		public static final class Builder implements Cloneable {
			private @Nullable Target target;
			private @Nullable InetAddress ip;
			private @Nullable String login;

			/**
			 * Creates a new builder.
			 *
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@CheckReturnValue
			public Builder() {
			}

			private Builder(@NotNull Source source) {
				this.target = source.target;
				this.ip = source.ip;
				this.login = source.login;
			}

			private Builder(@NotNull Builder builder) {
				this.target = builder.target;
				this.ip = builder.ip;
				this.login = builder.login;
			}

			/**
			 * Sets the identity of this source.
			 *
			 * @param target the identity of this source
			 * @return this builder
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@NotNull
			@Contract("_ -> this")
			public Builder target(@Nullable Target target) {
				this.target = target;
				return this;
			}

			/**
			 * Sets the IP address of this source.
			 *
			 * @param ip the IP address of this source
			 * @return this builder
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@NotNull
			@Contract("_ -> this")
			public Builder ip(@Nullable InetAddress ip) {
				this.ip = ip;
				return this;
			}

			/**
			 * Sets the IP address of this source.
			 *
			 * @param ip the IP address of this source
			 * @return this builder
			 * @throws UnknownHostException if the IP address is invalid
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@NotNull
			@Contract("_ -> this")
			public Builder ip(@Nullable String ip) throws UnknownHostException {
				this.ip = ip == null ? null : InetAddress.getByName(ip);
				return this;
			}

			/**
			 * Sets the name or ID of the streamer in the game.
			 *
			 * @param login the name or ID of the streamer in the game
			 * @return this builder
			 * @since 3.6.1
			 */
			@ApiStatus.AvailableSince("3.6.1")
			@NotNull
			@Contract("_ -> this")
			public Builder login(@Nullable String login) {
				this.login = login;
				return this;
			}

			// getters

			/**
			 * Gets the identity of this source.
			 *
			 * @return the identity of this source
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@Nullable
			@CheckReturnValue
			public Target target() {
				return target;
			}

			/**
			 * Gets the IP address of this source.
			 *
			 * @return the IP address of this source
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@Nullable
			@CheckReturnValue
			public InetAddress ip() {
				return ip;
			}

			/**
			 * Gets the name or ID of the streamer in the game.
			 *
			 * @return the name or ID of the streamer in the game
			 * @since 3.6.1
			 */
			@ApiStatus.AvailableSince("3.6.1")
			@Nullable
			@CheckReturnValue
			public String login() {
				return login;
			}

			// misc

			/**
			 * Creates a new builder object with the same parameters.
			 *
			 * @return cloned builder
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@SuppressWarnings("MethodDoesntCallSuperMethod")
			@Override
			public @NotNull Builder clone() {
				return new Builder(this);
			}

			/**
			 * Builds a new {@link Source} object.
			 *
			 * @return new Request
			 * @since 3.6.0
			 */
			@ApiStatus.AvailableSince("3.6.0")
			@NotNull
			@CheckReturnValue
			public Source build() {
				return new Source(this);
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
		private transient SocketManager originatingSocket;
		private int id = 0;
		private String effect;
		private String message;
		private String viewer;
		private @Nullable Integer cost;
		private Type type = Type.START;
		private Target @Nullable [] targets;
		private @Nullable Duration duration;
//		private Object @Nullable [] parameters;
		private @Nullable Source source;
		private @Nullable Object value;
		private @Nullable Integer quantity;
		private @Nullable String login;
		private @Nullable String password;
		private @Nullable Target player;

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
//			this.parameters = source.parameters;
			this.source = source.source;
			this.value = source.value;
			this.quantity = source.quantity;
			this.login = source.login;
			this.password = source.password;
			this.player = source.player;
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
//			this.parameters = builder.parameters;
			this.source = builder.source;
			this.value = builder.value;
			this.quantity = builder.quantity;
			this.login = builder.login;
			this.password = builder.password;
			this.player = builder.player;
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
		public Builder targets(@NotNull Target @Nullable ... targets) {
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
		public Builder originatingSocket(@Nullable SocketManager originatingSocket) {
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
//		@ApiStatus.AvailableSince("3.5.0")
//		@NotNull
//		@Contract("_ -> this")
//		public Builder parameters(Object @Nullable ... parameters) {
//			this.parameters = parameters;
//			return this;
//		}

		/**
		 * Sets the streamer through which the effect was requested.
		 *
		 * @param source streamer through which the effect was requested
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder source(@Nullable Source source) {
			this.source = source;
			return this;
		}

		/**
		 * Sets the value returned by a {@link Type#REMOTE_FUNCTION_RESULT remote function}.
		 *
		 * @param value remote function result
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder value(@Nullable Object value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the quantity of the item to be added or removed.
		 *
		 * @param quantity quantity of the item to be added or removed
		 * @return this builder
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@NotNull
		@Contract("_ -> this")
		public Builder quantity(@Nullable Integer quantity) {
			this.quantity = quantity;
			return this;
		}

		/**
		 * Sets the name or ID of the streamer in the game.
		 *
		 * @param login name or ID of the streamer in the game
		 * @return this builder
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@Contract("_ -> this")
		public Builder login(@Nullable String login) {
			this.login = login;
			return this;
		}

		/**
		 * Sets the submitted password.
		 *
		 * @param password submitted password
		 * @return this builder
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@NotNull
		@Contract("_ -> this")
		public Builder password(@Nullable String password) {
			this.password = password;
			return this;
		}

		/**
		 * Sets the {@link Type#PLAYER_INFO} data.
		 *
		 * @param player a player target
		 * @return this builder
		 * @since 3.6.2
		 */
		@ApiStatus.AvailableSince("3.6.2")
		@NotNull
		@Contract("_ -> this")
		public Builder player(@Nullable Target player) {
			this.player = player;
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
		public SocketManager originatingSocket() {
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
//		@ApiStatus.AvailableSince("3.5.0")
//		@CheckReturnValue
//		public Object @Nullable [] parameters() {
//			return parameters;
//		}

		/**
		 * Gets the streamer through which the effect was requested.
		 *
		 * @return streamer through which the effect was requested
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public Source source() {
			return source;
		}

		/**
		 * Gets the value returned by a {@link Type#REMOTE_FUNCTION_RESULT remote function}.
		 *
		 * @return remote function result
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public Object value() {
			return value;
		}

		/**
		 * Gets the quantity of the item to be added or removed.
		 *
		 * @return quantity of the item to be added or removed
		 * @since 3.6.0
		 */
		@ApiStatus.AvailableSince("3.6.0")
		@Nullable
		@CheckReturnValue
		public Integer quantity() {
			return quantity;
		}

		/**
		 * Gets the name or ID of the streamer in the game.
		 *
		 * @return name or ID of the streamer in the game
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@Nullable
		@CheckReturnValue
		public String login() {
			return login;
		}

		/**
		 * Gets the submitted password.
		 *
		 * @return submitted password
		 * @since 3.6.1
		 */
		@ApiStatus.AvailableSince("3.6.1")
		@Nullable
		@CheckReturnValue
		public String password() {
			return password;
		}

		/**
		 * Gets the {@link Type#PLAYER_INFO} data.
		 *
		 * @return a player target
		 * @since 3.6.2
		 */
		@ApiStatus.AvailableSince("3.6.2")
		@Nullable
		@CheckReturnValue
		public Target player() {
			return player;
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
