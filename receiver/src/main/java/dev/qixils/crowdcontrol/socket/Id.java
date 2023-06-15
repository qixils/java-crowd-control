package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

class Id {
	public final @NotNull String id;
	public final @NotNull IdType type;

	public Id(@NotNull String id, @Nullable IdType type) {
		this.id = id.toLowerCase(Locale.ENGLISH);
		this.type = ExceptionUtil.validateNotNullElse(type, IdType.EFFECT);
	}
}
