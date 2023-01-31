package dev.qixils.crowdcontrol.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StringUtils {
	public static @NotNull String repr(@Nullable String string) {
		if (string == null) return "null";
		return '\'' + string + '\'';
	}
}
