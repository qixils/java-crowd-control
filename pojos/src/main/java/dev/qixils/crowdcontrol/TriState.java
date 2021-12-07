package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A boolean value that may be unknown.
 */
public enum TriState {
	/**
	 * The value is {@code true}.
	 */
	TRUE(true),
	/**
	 * The value is {@code false}.
	 */
	FALSE(false),
	/**
	 * The value is unknown.
	 */
	UNKNOWN(null);

	private final Boolean value;

	TriState(Boolean value) {
		this.value = value;
	}

	/**
	 * Returns the {@link TriState} equivalent of the given {@link Boolean}.
	 *
	 * @param value the {@link Boolean} to convert
	 * @return the equivalent {@link TriState}
	 */
	@NotNull
	public static TriState fromBoolean(@Nullable Boolean value) {
		if (value == null)
			return UNKNOWN;
		else if (value)
			return TRUE;
		else
			return FALSE;
	}

	/**
	 * Returns the {@link Boolean} equivalent of this {@code TriState}.
	 *
	 * @return boolean equivalent
	 */
	@Nullable
	public Boolean getBoolean() {
		return value;
	}

	/**
	 * Returns the primitive boolean equivalent of this {@code TriState}.
	 *
	 * @return primitive boolean equivalent
	 */
	public boolean getPrimitiveBoolean() {
		return value != null && value;
	}
}
