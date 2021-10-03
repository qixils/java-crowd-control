package dev.qixils.crowdcontrol.socket;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * An outgoing packet to the Crowd Control TCP server carrying information in response to an {@link Request incoming packet}.
 * @see Request
 */
public final class Response {
	private final int id;
	@SerializedName("status")
	private final ResultType type;
	private final String message;
	private final long timeRemaining; // millis

	/**
	 * Constructs a response to a {@link Request} given its ID, the result of executing the effect,
	 * and an associated message.
	 * @param id Request ID
	 * @param type result of execution
	 * @param message result message
	 * @param timeRemaining remaining duration for the referenced effect in milliseconds
	 */
	@CheckReturnValue
	public Response(int id, @NotNull Response.ResultType type, @NotNull String message, long timeRemaining) {
		this.id = id;
		this.type = Objects.requireNonNull(type, "type");
		this.message = Objects.requireNonNull(message, "message");
		this.timeRemaining = timeRemaining;
	}

	/**
	 * Gets the ID of the outgoing packet. Corresponds to a unique transaction.
	 * @return packet ID
	 */
	@CheckReturnValue
	public int getId() {
		return id;
	}

	/**
	 * Gets the result of executing an effect.
	 * @return effect result
	 */
	@NotNull
	@CheckReturnValue
	public Response.ResultType getResultType() {
		return type;
	}

	/**
	 * Gets the message that will be delivered along with the result.
	 * @return result message
	 */
	@NotNull
	@CheckReturnValue
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the milliseconds left until the referenced effect ends.
	 * @return effect duration in milliseconds
	 */
	@CheckReturnValue
	public long getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Outputs this object as a JSON string for use in the server connection.
	 * @return JSON string
	 */
	@NotNull
	@CheckReturnValue
	public String toJSON() {
		return EnumOrdinalAdapter.GSON.toJson(this);
	}

	/**
	 * Gets a mutable {@link Builder} representing this Response.
	 * @return new builder
	 */
	@NotNull
	@CheckReturnValue
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Creates an empty {@link Builder} representing a Response.
	 * @return new empty builder
	 */
	@NotNull
	@CheckReturnValue
	public static Builder builder() {
		return new Builder();
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
		 * The effect is unavailable for use. Treated the same as {@link #FAILURE} by Crowd Control.
		 */
		UNAVAILABLE,
		/**
		 * The effect is momentarily unavailable but may be retried in a few seconds.
		 */
		RETRY,
		/**
		 * The effect has been queued for execution after the current one ends.
		 * <p>
		 * <i>This value is intended for use by the library.</i>
		 */
		QUEUE,
		/**
		 * The effect triggered successfully and is now active until it ends.
		 * <p>
		 * <i>This value is intended for use by the library only. {@link #SUCCESS} should generally be used instead.</i>
		 */
		RUNNING,
		/**
		 * The timed effect has been paused and is now waiting.
		 * <p>
		 * <i>This value is intended for use by the library.</i>
		 */
		PAUSED,
		/**
		 * The timed effect has been resumed and is counting down again.
		 * <p>
		 * <i>This value is intended for use by the library.</i>
		 */
		RESUMED,
		/**
		 * The timed effect has finished.
		 * <p>
		 * <i>This value is intended for use by the library.</i>
		 */
		FINISHED
	}

	/**
	 * Mutable builder for the immutable {@link Response} class.
	 */
	public static class Builder {
		private int id;
		private ResultType type;
		private String message;
		private long timeRemaining;

		/**
		 * Instantiates an empty builder.
		 * @see Builder
		 */
		@CheckReturnValue
		public Builder(){}

		/**
		 * Creates a new builder using the data from a {@link Response}.
		 * @param source source for a new builder
		 */
		@CheckReturnValue
		public Builder(@NotNull Response source) {
			Objects.requireNonNull(source, "source cannot be null");
			this.id = source.id;
			this.message = source.message;
			this.type = source.type;
			this.timeRemaining = source.timeRemaining;
		}

		/**
		 * Creates a new builder representing the {@link Response} to a {@link Request}.
		 * @param request request to respond to
		 */
		@CheckReturnValue
		public Builder(@NotNull Request request) {
			this.id = Objects.requireNonNull(request, "request cannot be null").getId();
		}

		/**
		 * Creates a new builder representing the {@link Response} to a request ID.
		 * @param id request to respond to
		 */
		@CheckReturnValue
		public Builder(int id) {
			this.id = id;
		}

		/**
		 * Sets the ID of the request being responded to.
		 * @param id request ID
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder id(int id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the type of result being returned.
		 * @param type result type
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder type(@Nullable ResultType type) {
			this.type = type;
			if (type != null && message == null)
				message = type.name();
			return this;
		}

		/**
		 * Sets the message describing or explaining the response.
		 * <br>Useful for explaining why an effect failed to apply.
		 * @param message response message
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder message(@Nullable String message) {
			this.message = message;
			return this;
		}

		/**
		 * Sets the time left on the referenced effect in milliseconds.
		 * @param timeRemaining time in milliseconds
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(long timeRemaining) {
			if (timeRemaining < 0)
				throw new IllegalArgumentException("'timeRemaining' must be positive");
			this.timeRemaining = timeRemaining;
			return this;
		}

		/**
		 * Sets the time left on the referenced effect.
		 * @param timeRemaining effect duration
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Duration timeRemaining) {
			return timeRemaining != null ? timeRemaining(timeRemaining.toMillis()) : this;
		}

		/**
		 * Sets the time at which the referenced effect will end.
		 * @param endEffectAt time to end effect
		 * @return this builder
		 */
		@NotNull
		@Contract("_ -> this")
		public Builder timeRemaining(@Nullable Temporal endEffectAt) {
			return endEffectAt != null
					? timeRemaining(ChronoUnit.MILLIS.between(LocalDateTime.now(), endEffectAt))
					: this;
		}

		/**
		 * Builds a new {@link Response} object.
		 * @return new Response
		 */
		@NotNull
		@CheckReturnValue
		public Response build() {
			return new Response(id, type, message, timeRemaining);
		}
	}
}
