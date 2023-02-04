package dev.qixils.crowdcontrol.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for working with strings.
 */
public final class StringUtils {

	private StringUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a string representation of the given string.
	 * If the string is null, returns "null".
	 * Else, returns the string surrounded by single quotes.
	 *
	 * @param string string to represent
	 * @return string representation
	 */
	public static @NotNull String repr(@Nullable String string) {
		if (string == null) return "null";
		return '\'' + string + '\'';
	}
}
