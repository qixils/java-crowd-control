package dev.qixils.crowdcontrol.socket;

import com.google.gson.annotations.SerializedName;
import dev.qixils.crowdcontrol.CrowdControl;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An outgoing packet to the Crowd Control TCP server carrying information in response to an {@link Request incoming packet}.
 * @see Request
 */
public class Response {
	private final int id;
	@SerializedName("status")
	private ResultType type;
	private String message;

	/**
	 * Constructs a response to a {@link Request} given its ID. Defaults to {@link ResultType#UNAVAILABLE}.
	 * @param id Request ID
	 */
	public Response(int id) {
		this(id, ResultType.UNAVAILABLE);
	}

	/**
	 * Constructs a response to a {@link Request} given its ID and a {@link Result}.
	 * @param id Request ID
	 * @param result result of execution
	 */
	public Response(int id, @NotNull Response.Result result) {
		this(id, Objects.requireNonNull(result, "result").getType(), result.getMessage());
	}

	/**
	 * Constructs a response to a {@link Request} given its ID and the result of executing the effect.
	 * @param id Request ID
	 * @param type result of execution
	 */
	public Response(int id, @NotNull Response.ResultType type) {
		this(id, Objects.requireNonNull(type, "type"), type.name());
	}

	/**
	 * Constructs a response to a {@link Request} given its ID, the result of executing the effect,
	 * and an associated message.
	 * @param id Request ID
	 * @param type result of execution
	 * @param message result message
	 */
	public Response(int id, @NotNull Response.ResultType type, @NotNull String message) {
		this.id = id;
		this.type = Objects.requireNonNull(type, "type");
		this.message = Objects.requireNonNull(message, "message");
	}

	/**
	 * Gets the ID of the outgoing packet. Corresponds to a unique transaction.
	 * @return packet ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gets the result of executing an effect.
	 * @return effect result
	 */
	@NotNull
	public Response.ResultType getResultType() {
		return type;
	}

	/**
	 * Gets the message that will be delivered along with the result.
	 * @return result message
	 */
	@NotNull
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the result from executing an effect.
	 * @param type effect result
	 */
	public void setResultType(@NotNull Response.ResultType type) {
		this.type = Objects.requireNonNull(type, "result");
	}

	/**
	 * Sets the message that will be delivered along with the result.
	 * @param message result message
	 */
	public void setMessage(@NotNull String message) {
		this.message = Objects.requireNonNull(message, "message");
	}

	/**
	 * Outputs this object as a JSON string for use in the server connection.
	 * @return JSON string
	 */
	@NotNull
	public String toJSON() {
		return CrowdControl.GSON.toJson(this);
	}

	/**
	 * The result of processing an incoming packet.
	 */
	public enum ResultType {
		/**
		 * The effect was applied successfully.
		 */
		SUCCESS,
		/**
		 * The effect failed to be applied. Will refund the purchaser.
		 */
		FAILURE,
		/**
		 * The effect is currently unavailable. Treated the same as {@link #FAILURE} by Crowd Control.
		 */
		UNAVAILABLE,
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 */
		RETRY
	}

	/**
	 * The result of executing a {@link Request}.
	 */
	public static class Result {
		private final ResultType type;
		private final String message;

		/**
		 * Creates a wrapper with a result and a message defaulting to the name of the result.
		 * @param type effect result
		 */
		public Result(@NotNull Response.ResultType type) {
			this(Objects.requireNonNull(type, "type"), type.name());
		}

		/**
		 * Creates a wrapper with a result and a message.
		 * @param type effect result
		 * @param message effect message
		 */
		public Result(@NotNull Response.ResultType type, String message) {
			this.type = Objects.requireNonNull(type, "type");
			this.message = Objects.requireNonNull(message, "message");
		}

		/**
		 * Gets the result of the effect.
		 * @return effect result
		 */
		@NotNull
		public Response.ResultType getType() {
			return type;
		}

		/**
		 * Gets the message associated with the effect's result.
		 * @return result message
		 */
		@NotNull
		public String getMessage() {
			return message;
		}
	}
}
