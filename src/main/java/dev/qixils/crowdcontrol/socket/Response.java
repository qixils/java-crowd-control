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
	private Result result;
	private String message;

	/**
	 * Constructs a response to a {@link Request} given its ID. Defaults to {@link Result#UNAVAILABLE}.
	 * @param id Request ID
	 */
	public Response(int id) {
		this(id, Result.UNAVAILABLE);
	}

	/**
	 * Constructs a response to a {@link Request} given its ID and a {@link ResultWrapper}.
	 * @param id Request ID
	 * @param result result of execution
	 */
	public Response(int id, @NotNull ResultWrapper result) {
		this(id, Objects.requireNonNull(result, "result").getResult(), result.getMessage());
	}

	/**
	 * Constructs a response to a {@link Request} given its ID and the result of executing the effect.
	 * @param id Request ID
	 * @param result result of execution
	 */
	public Response(int id, @NotNull Result result) {
		this(id, Objects.requireNonNull(result, "status"), result.name());
	}

	/**
	 * Constructs a response to a {@link Request} given its ID, the result of executing the effect,
	 * and an associated message.
	 * @param id Request ID
	 * @param result result of execution
	 * @param message result message
	 */
	public Response(int id, @NotNull Result result, @NotNull String message) {
		this.id = id;
		this.result = Objects.requireNonNull(result, "status");
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
	public Result getResult() {
		return result;
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
	 * @param result effect result
	 */
	public void setResult(@NotNull Result result) {
		this.result = Objects.requireNonNull(result, "result");
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
	public enum Result {
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
	 * A wrapper around {@link Result} which includes a message.
	 */
	public static class ResultWrapper {
		private final Response.Result result;
		private final String message;

		/**
		 * Creates a wrapper with a result and a message defaulting to the name of the result.
		 * @param result effect result
		 */
		public ResultWrapper(@NotNull Response.Result result) {
			this(Objects.requireNonNull(result, "result"), result.name());
		}

		/**
		 * Creates a wrapper with a result and a message.
		 * @param result effect result
		 * @param message effect message
		 */
		public ResultWrapper(@NotNull Response.Result result, String message) {
			this.result = Objects.requireNonNull(result, "result");
			this.message = Objects.requireNonNull(message, "message");
		}

		/**
		 * Gets the result of the effect.
		 * @return effect result
		 */
		@NotNull
		public Response.Result getResult() {
			return result;
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
