package dev.qixils.crowdcontrol.socket;

/**
 * An object that can be serialized into JSON.
 */
public interface JsonObject {
	/**
	 * Converts this object to its JSON representation.
	 *
	 * @return JSON string
	 */
	String toJSON();
}
