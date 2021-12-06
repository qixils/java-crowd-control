package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 *
 * @see Response
 */
public final class Request implements JsonObject {
	transient Socket originatingSocket;
	private int id;
	@SerializedName("code")
	private String effect; // more sensible variable name for this library
	private String message;
	private String viewer;
	private Integer cost; // I believe this is nullable
	private Type type;
	private Target[] targets;

	/**
	 * Instantiates an empty {@link Request}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	Request() {
	}

	/**
	 * Instantiates a {@link Request} object from a {@link Builder}.
	 *
	 * @param builder the {@link Builder} to use
	 */
	public Request(Request.@NotNull Builder builder) {
		this.type = Objects.requireNonNull(builder.type, "type cannot be null");
		this.id = builder.id;
		this.effect = builder.effect;
		this.message = builder.message;
		this.viewer = builder.viewer;
		this.cost = builder.cost;
		this.targets = builder.targets;
	}

	/**
	 * Creates a {@link Request} object from JSON.
	 *
	 * @param json input json data from the Crowd Control TCP server
	 * @return a new Request object
	 * @throws JsonSyntaxException the JSON failed to be parsed
	 */
	@NotNull
	@CheckReturnValue
	public static Request fromJSON(@NotNull String json) throws JsonSyntaxException {
		return ByteAdapter.GSON.fromJson(Objects.requireNonNull(json, "json"), Request.class);
	}

	/**
	 * Gets the ID of the incoming packet. Corresponds to a unique transaction.
	 *
	 * @return packet ID
	 */
	@CheckReturnValue
	public int getId() {
		return id;
	}

	/**
	 * Gets the message from the incoming packet.
	 *
	 * @return message
	 */
	@Nullable
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the name of the effect to play.
	 *
	 * @return effect name
	 */
	@NotNull
	@CheckReturnValue
	public String getEffect() {
		return effect;
	}

	/**
	 * Gets the name of the viewer who triggered the effect.
	 *
	 * @return viewer name
	 */
	@NotNull
	@CheckReturnValue
	public String getViewer() {
		return viewer;
	}

	/**
	 * Gets the cost of the effect specified in this Request.
	 *
	 * @return effect cost
	 */
	@Nullable
	@CheckReturnValue
	public Integer getCost() {
		return cost;
	}

	/**
	 * Gets the {@link Type Type} of the request.
	 *
	 * @return request type
	 */
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
	 */
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
	 */
	@CheckReturnValue
	public boolean isGlobal() {
		return targets == null || targets.length == 0;
	}

	/**
	 * Outputs this object as a JSON string.
	 *
	 * @return JSON string
	 */
	@NotNull
	@CheckReturnValue
	public String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}

	/**
	 * Creates a {@link Builder} for this {@link Request}.
	 *
	 * @return a new {@link Builder}
	 */
	@NotNull
	@CheckReturnValue
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Creates a {@link dev.qixils.crowdcontrol.socket.Response.Builder} for
	 * a {@link Response} to this request.
	 *
	 * @return new response builder
	 */
	@CheckReturnValue
	public Response.Builder buildResponse() {
		return new Response.Builder(this);
	}

	@Override
	@CheckReturnValue
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Request request = (Request) o;
		return getId() == request.getId()
				&& Objects.equals(getEffect(), request.getEffect())
				&& Objects.equals(getMessage(), request.getMessage())
				&& Objects.equals(getViewer(), request.getViewer())
				&& Objects.equals(getCost(), request.getCost())
				&& getType() == request.getType()
				&& Arrays.equals(getTargets(), request.getTargets());
	}

	@Override
	@CheckReturnValue
	public int hashCode() {
		int result = Objects.hash(getId(), getEffect(), getMessage(), getViewer(), getCost(), getType());
		result = 31 * result + Arrays.hashCode(getTargets());
		return result;
	}

	/**
	 * The type of incoming packet.
	 */
	public enum Type implements ByteObject {
		/**
		 * Indicates that you should simulate the starting of an effect (i.e. test if it's available)
		 * but should not actually start the effect.
		 */
		TEST,
		/**
		 * Indicates that you should start an effect, if available.
		 */
		START,
		/**
		 * Indicates that you should stop an effect.
		 */
		STOP,
		/**
		 * Indicates that a streamer is attempting to log in to the Crowd Control server.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 */
		LOGIN((byte) 0xF0),
		/**
		 * This packet's sole purpose is to establish that the connection with the
		 * Crowd Control server has not been dropped.
		 * <p>
		 * This value is only used internally by the library. You will not encounter this value
		 * and should assume it does not exist.
		 */
		KEEP_ALIVE((byte) 0xFF);

		private static final Map<Byte, Type> BY_BYTE;

		static {
			Map<Byte, Type> map = new HashMap<>(values().length);
			for (Type type : values())
				map.put(type.encodedByte, type);
			BY_BYTE = map;
		}

		private final byte encodedByte;

		Type(byte encodedByte) {
			this.encodedByte = encodedByte;
		}

		Type() {
			this.encodedByte = (byte) ordinal();
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 *
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 */
		@CheckReturnValue
		public static @Nullable Type from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}

		@CheckReturnValue
		public byte getEncodedByte() {
			return encodedByte;
		}
	}

	/**
	 * A recipient of an effect.
	 * <p>
	 * This corresponds to a Twitch streamer connected to the Crowd Control server.
	 */
	public final static class Target {
		private int id;
		private String name;
		private String avatar;

		/**
		 * Instantiates an empty {@link Target}.
		 * <p>
		 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
		 */
		Target() {
		}

		/**
		 * Instantiates a {@link Target} with the given streamer information.
		 *
		 * @param id     streamer ID
		 * @param name   streamer name
		 * @param avatar streamer avatar
		 */
		public Target(int id, @NotNull String name, @NotNull String avatar) {
			this.id = id;
			this.name = name;
			this.avatar = avatar;
		}

		/**
		 * The recipient's Twitch ID.
		 *
		 * @return Twitch ID
		 */
		@CheckReturnValue
		public int getId() {
			return id;
		}

		/**
		 * The recipient's name on Twitch.
		 *
		 * @return Twitch username
		 */
		@NotNull
		@CheckReturnValue
		public String getName() {
			return name;
		}

		/**
		 * Gets the URL of the recipient's avatar on Twitch.
		 *
		 * @return Twitch avatar URL
		 */
		@NotNull
		@CheckReturnValue
		public String getAvatar() {
			return avatar;
		}

		@Override
		@CheckReturnValue
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Target target = (Target) o;
			return getId() == target.getId() && getName().equals(target.getName()) && getAvatar().equals(target.getAvatar());
		}

		@Override
		@CheckReturnValue
		public int hashCode() {
			return Objects.hash(getId(), getName(), getAvatar());
		}
	}

	/**
	 * Mutable builder for the immutable {@link Request} class.
	 */
	public static class Builder implements Cloneable {
		private int id = -1;
		private String effect;
		private String message;
		private String viewer;
		private @Nullable Integer cost;
		private Type type;
		private Target[] targets;

		/**
		 * Creates a new builder using the data from a {@link Request}.
		 *
		 * @param source source for a new builder
		 */
		@CheckReturnValue
		public Builder(@NotNull Request source) {
			Objects.requireNonNull(source, "source cannot be null");
			this.id = source.id;
			this.effect = source.effect;
			this.message = source.message;
			this.viewer = source.viewer;
			this.cost = source.cost;
			this.type = source.type;
			this.targets = source.targets;
		}

		/**
		 * Creates a new builder.
		 */
		@CheckReturnValue
		public Builder() {
		}

		/**
		 * Sets the type of result being returned.
		 *
		 * @param type result type
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder type(@Nullable Type type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the message describing or explaining the request.
		 *
		 * @param message request message
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder message(@Nullable String message) {
			this.message = message;
			return this;
		}

		/**
		 * Sets the effect being requested.
		 *
		 * @param effect requested effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder effect(@Nullable String effect) {
			this.effect = effect;
			return this;
		}

		/**
		 * Sets the viewer requesting the effect.
		 *
		 * @param viewer viewer requesting the effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder viewer(@Nullable String viewer) {
			this.viewer = viewer;
			return this;
		}

		/**
		 * Sets the cost of the effect.
		 *
		 * @param cost cost of the effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder cost(@Nullable Integer cost) {
			this.cost = cost;
			return this;
		}

		/**
		 * Sets the targets of the effect.
		 *
		 * @param targets targets of the effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder targets(Target @Nullable [] targets) {
			this.targets = targets;
			return this;
		}

		/**
		 * Sets the ID of the request.
		 *
		 * @param id request ID
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Request.Builder id(int id) {
			this.id = id;
			return this;
		}

		/**
		 * Builds a new {@link Request} object.
		 *
		 * @return new Request
		 */
		@NotNull
		@CheckReturnValue
		public Request build() {
			return new Request(this);
		}

		/**
		 * Creates a new {@link Response.Builder} object with the same parameters.
		 *
		 * @return cloned builder
		 */
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		public Request.Builder clone() {
			return new Request.Builder().type(type).message(message).effect(effect).viewer(viewer).cost(cost).targets(targets);
		}
	}
}
