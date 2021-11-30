package dev.qixils.crowdcontrol.socket;

/**
 * An object that is represented by a byte in JSON serialization.
 */
public interface ByteObject {
	/**
	 * Gets the byte that this object is represented by in JSON serialization.
	 *
	 * @return encoded byte
	 */
	byte getEncodedByte();
}
