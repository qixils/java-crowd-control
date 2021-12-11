package dev.qixils.crowdcontrol.exceptions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
		validateNotNull(potentialCause, "potentialCause");
		if (exception == null) return false;
		if (potentialCause.isInstance(exception)) return true;
		return isCause(potentialCause, exception.getCause());
	}

	/**
	 * Validates that the provided object is not null.
	 *
	 * @param object object to validate
	 * @param <T>    type of object to accept and return
	 * @return the object if not null
	 * @throws IllegalArgumentException if the object is null
	 */
	@Contract("null -> fail; !null -> !null")
	public static <T> T validateNotNull(@Nullable T object) {
		return validateNotNull(object, null);
	}

	/**
	 * Validates that the provided object is not null.
	 *
	 * @param object       object to validate
	 * @param variableName name of the variable being validated
	 * @param <T>          type of object to accept and return
	 * @return the object if not null
	 * @throws IllegalArgumentException if the object is null
	 */
	@Contract("null, _ -> fail; !null, _ -> !null")
	public static <T> T validateNotNull(@Nullable T object, @Nullable String variableName) {
		if (object == null)
			throw new IllegalArgumentException(Objects.requireNonNullElse(variableName, "Object") + " cannot be null");
		return object;
	}
}
