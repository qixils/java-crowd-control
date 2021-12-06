package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

/**
 * An object that can be serialized into JSON.
 */
public interface JsonObject {
	/**
	 * Obtains a JSON object from an input stream.
	 *
	 * @param input      an input stream
	 * @param jsonMapper a function that maps a JSON string to a POJO
	 * @param <T>        the type of the POJO
	 * @return the parsed POJO
	 * @throws IOException if an I/O error occurs in the input stream
	 */
	@Nullable
	@CheckReturnValue
	static <T> T fromInputStream(@NotNull InputStreamReader input, Function<@NotNull String, @Nullable T> jsonMapper) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] results = new char[1];
		int bytes_read = input.read(results);
		while (results[0] != 0x00 && bytes_read == 1) {
			sb.append(results[0]);
			bytes_read = input.read(results);
		}

		String inJSON = sb.toString();
		if (inJSON.isBlank())
			return null;
		return jsonMapper.apply(inJSON);
	}

	/**
	 * Converts this object to its JSON representation.
	 *
	 * @return JSON string
	 */
	@NotNull
	@CheckReturnValue
	String toJSON();
}
