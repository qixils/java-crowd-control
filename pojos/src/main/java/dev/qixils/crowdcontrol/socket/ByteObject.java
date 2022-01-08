package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;

/**
 * An object that is represented by a byte in JSON serialization.
 *
 * @since 3.1.0
 */
@ApiStatus.AvailableSince("3.1.0")
public interface ByteObject {
	/**
	 * Gets the byte that this object is represented by in JSON serialization.
	 *
	 * @return encoded byte
	 * @since 3.1.0
	 */
	@ApiStatus.AvailableSince("3.1.0")
	byte getEncodedByte();
}
