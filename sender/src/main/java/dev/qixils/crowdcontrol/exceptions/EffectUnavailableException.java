package dev.qixils.crowdcontrol.exceptions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The provided exception is known to be unavailable to the receiver(s).
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public class EffectUnavailableException extends CrowdControlException {
	/**
	 * Constructs a new exception with {@code null} as its detail message.
	 * The cause is not initialized, and may subsequently be initialized by a
	 * call to {@link #initCause}.
	 *
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public EffectUnavailableException() {
		super();
	}

	/**
	 * Constructs a new exception with the specified detail message.  The
	 * cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for
	 *                later retrieval by the {@link #getMessage()} method.
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public EffectUnavailableException(@Nullable String message) {
		super(message);
	}
}
