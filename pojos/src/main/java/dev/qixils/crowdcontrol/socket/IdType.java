package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.HashMap;
import java.util.Map;

/**
 * A type of effect identifier.
 *
 * @since 3.7.0
 */
@ApiStatus.AvailableSince("3.7.0")
public enum IdType implements ByteObject {

	/**
	 * An effect.
	 */
	EFFECT,
	/**
	 * A group of effects.
	 */
	GROUP,
	/**
	 * A category of effects.
	 */
	CATEGORY,
	;

	private final byte encodedByte;

	IdType() {
		this.encodedByte = (byte) ordinal();
	}

	IdType(byte encodedByte) {
		this.encodedByte = encodedByte;
	}

	@Override
	public byte getEncodedByte() {
		return encodedByte;
	}

	private static final @NotNull Map<@NotNull Byte, @NotNull IdType> BY_BYTE;

	static {
		Map<Byte, IdType> map = new HashMap<>(values().length);
		for (IdType type : values())
			map.put(type.getEncodedByte(), type);
		BY_BYTE = map;
	}

	/**
	 * Gets an ID type from its corresponding JSON encoding.
	 *
	 * @param encodedByte byte used in JSON encoding
	 * @return corresponding IdType if applicable
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	@ApiStatus.Internal
	@CheckReturnValue
	public static @Nullable IdType from(byte encodedByte) {
		return BY_BYTE.get(encodedByte);
	}
}
