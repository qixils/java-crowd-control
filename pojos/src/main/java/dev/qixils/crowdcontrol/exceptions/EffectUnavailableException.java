package dev.qixils.crowdcontrol.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * The provided exception is known to be unavailable to the receiver(s).
 */
public class EffectUnavailableException extends CrowdControlException {
	/**
	 * Constructs a new exception with {@code null} as its detail message.
	 * The cause is not initialized, and may subsequently be initialized by a
	 * call to {@link #initCause}.
	 */
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
	 */
	public EffectUnavailableException(@Nullable String message) {
		super(message);
	}
}
