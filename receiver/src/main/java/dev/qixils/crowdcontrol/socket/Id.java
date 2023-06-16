package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static dev.qixils.crowdcontrol.util.StringUtils.repr;

class Id {
	public final @NotNull String id;
	public final @NotNull IdType type;

	public Id(@NotNull String id, @Nullable IdType type) {
		this.id = id.toLowerCase(Locale.ENGLISH);
		this.type = ExceptionUtil.validateNotNullElse(type, IdType.EFFECT);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Id other = (Id) o;
		return id.equals(other.id) && type == other.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type);
	}

	@Override
	public String toString() {
		return "Id{" +
				"id=" + repr(id) +
				", type=" + type +
				'}';
	}
}
