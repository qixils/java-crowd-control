package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 * @see Response
 */
public final class Request {
	private int id;
	@SerializedName("code")
	private String effect; // more sensible variable name for this library
	private String message;
	private Object[] parameters; // mostly used for C# code I believe, maybe not of much use here
	private String viewer;
	private Integer cost; // I believe this is nullable
	private Type type;
	private Target[] targets;
	transient Socket originatingSocket;

	/**
	 * Instantiates an empty {@link Request}.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	Request(){}

	/**
	 * Gets the ID of the incoming packet. Corresponds to a unique transaction.
	 * @return packet ID
	 */
	@CheckReturnValue
	public int getId() {
		return id;
	}

	/**
	 * Gets the message from the incoming packet.
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the name of the effect to play.
	 * @return effect name
	 */
	@NotNull
	@CheckReturnValue
	public String getEffect() {
		return effect;
	}

	/**
	 * Gets the arguments supplied by the C# Crowd Control pack
	 * @return effect parameters
	 */
	@NotNull
	@CheckReturnValue
	public Object[] getParameters() {
		return parameters;
	}

	/**
	 * Gets the name of the viewer who triggered the effect.
	 * @return viewer name
	 */
	@NotNull
	@CheckReturnValue
	public String getViewer() {
		return viewer;
	}

	/**
	 * Gets the cost of the effect specified in this Request.
	 * @return effect cost
	 */
	@Nullable
	@CheckReturnValue
	public Integer getCost() {
		return cost;
	}

	/**
	 * Gets the {@link Type Type} of the request.
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
	 * @return possibly empty array of {@link Target}
	 */
	public Target @NotNull[] getTargets() {
		if (targets == null)
			targets = new Target[0];
		return targets;
	}

	/**
	 * Determines if this Request is triggering an effect for all users.
	 * @return if the triggered effect is global
	 */
	public boolean isGlobal() {
		return targets == null || targets.length == 0;
	}

	/**
	 * Creates a {@link dev.qixils.crowdcontrol.socket.Response.Builder} for
	 * a {@link Response} to this request.
	 * @return new response builder
	 */
	public Response.Builder buildResponse() {
		return new Response.Builder(this);
	}

	/**
	 * Creates a {@link Request} object from JSON.
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
		Target(){}

		/**
		 * The recipient's Twitch ID.
		 * @return Twitch ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * The recipient's name on Twitch.
		 * @return Twitch username
		 */
		@NotNull
		public String getName() {
			return name;
		}

		/**
		 * Gets the URL of the recipient's avatar on Twitch.
		 * @return Twitch avatar URL
		 */
		@NotNull
		public String getAvatar() {
			return avatar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Target target = (Target) o;
			return getId() == target.getId() && getName().equals(target.getName()) && getAvatar().equals(target.getAvatar());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId(), getName(), getAvatar());
		}
	}

	/**
	 * The type of incoming packet.
	 */
	public enum Type {
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
		 * Gets the byte that this type is represented by in JSON encoding.
		 * @return encoded byte
		 */
		public byte getEncodedByte() {
			return encodedByte;
		}

		/**
		 * Gets a packet type from its corresponding JSON encoding.
		 * @param encodedByte byte used in JSON encoding
		 * @return corresponding Type if applicable
		 */
		public static @Nullable Type from(byte encodedByte) {
			return BY_BYTE.get(encodedByte);
		}
	}
}
