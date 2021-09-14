package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 * @see Response
 */
public class Request {
	// TODO: test if these can be final, or if that causes serialization to fail
	protected int id;
	@SerializedName("code")
	protected String effect; // more sensible variable name for this library
	protected String viewer;
	protected Type type;

	/**
	 * Instantiates a basic request packet which represents an effect to be played.
	 * @param id ID of the packet
	 * @param effect name of the effect to play
	 * @param viewer viewer who triggered the effect
	 * @param type type of request
	 */
	public Request(int id, @NotNull String effect, @NotNull String viewer, @NotNull Type type) {
		this.id = id;
		this.effect = Objects.requireNonNull(effect, "effect");
		this.viewer = Objects.requireNonNull(viewer, "viewer");
		this.type = Objects.requireNonNull(type, "type");
	}

	/**
	 * Gets the ID of the incoming packet. Corresponds to a unique transaction.
	 * @return packet ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gets the name of the effect to play.
	 * @return effect name
	 */
	@NotNull
	public String getEffect() {
		return effect;
	}

	/**
	 * Gets the name of the viewer who triggered the effect.
	 * @return viewer name
	 */
	@NotNull
	public String getViewer() {
		return viewer;
	}

	/**
	 * Gets the {@link Type Type} of the request.
	 * @return request type
	 */
	@NotNull
	public Type getType() {
		return type;
	}

	/**
	 * Creates a {@link Request} object from JSON.
	 * @param json input json data from the Crowd Control TCP server
	 * @return a new Request object
	 * @throws JsonSyntaxException the JSON failed to be parsed
	 */
	@NotNull
	public static Request fromJSON(@NotNull String json) throws JsonSyntaxException {
		return EnumOrdinalAdapter.GSON.fromJson(Objects.requireNonNull(json, "json"), Request.class);
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
		STOP
	}
}
