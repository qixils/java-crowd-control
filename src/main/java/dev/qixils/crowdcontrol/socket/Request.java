package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.Objects;

/**
 * An incoming packet from the Crowd Control TCP server which represents an effect to be played.
 * @see Response
 */
public final class Request {
	private int id;
	@SerializedName("code")
	private String effect; // more sensible variable name for this library
	private Object[] parameters; // dunno what this is for atm
	private String viewer;
	private Integer cost; // i believe this is nullable
	private Type type;

	/**
	 * Instantiates an empty Request.
	 * <p>
	 * Used internally by the library, specifically for {@link com.google.gson.Gson} deserialization.
	 */
	Request(){}

	/**
	 * Instantiates a basic request packet which represents an effect to be played.
	 * @param id ID of the packet
	 * @param effect name of the effect to play
	 * @param parameters arguments supplied by the C# Crowd Control pack
	 * @param viewer viewer who triggered the effect
	 * @param cost optional cost of the effect
	 * @param type type of request
	 */
	@CheckReturnValue
	public Request(int id, @NotNull String effect, @Nullable Object[] parameters, @NotNull String viewer, @Nullable Integer cost, @NotNull Type type) {
		this.id = id;
		this.effect = Objects.requireNonNull(effect, "effect");
		this.parameters = Objects.requireNonNullElseGet(parameters, () -> new Object[0]);
		this.viewer = Objects.requireNonNull(viewer, "viewer");
		this.cost = cost;
		this.type = Objects.requireNonNull(type, "type");
	}

	/**
	 * Gets the ID of the incoming packet. Corresponds to a unique transaction.
	 * @return packet ID
	 */
	@CheckReturnValue
	public int getId() {
		return id;
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
	 * Creates a {@link Request} object from JSON.
	 * @param json input json data from the Crowd Control TCP server
	 * @return a new Request object
	 * @throws JsonSyntaxException the JSON failed to be parsed
	 */
	@NotNull
	@CheckReturnValue
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
