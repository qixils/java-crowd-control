package dev.qixils.crowdcontrol.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class containing common methods relating to exceptions.
 */
public class ExceptionUtil {
	private ExceptionUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Helper method for determining if a provided exception class is part of an exception's stacktrace.
	 *
	 * @param potentialCause class to search for in stacktrace
	 * @param exception      exception to be searched
	 * @return true if the exception class is found
	 */
	public static boolean isCause(@NotNull Class<? extends Throwable> potentialCause, @Nullable Throwable exception) {
		if (exception == null) return false;
		if (potentialCause.isInstance(exception)) return true;
		return isCause(potentialCause, exception.getCause());
	}
}
