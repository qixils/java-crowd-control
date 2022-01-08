package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

/**
 * An object that can be serialized into JSON.
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public interface JsonObject {
	/**
	 * Obtains a JSON object from an input stream.
	 *
	 * @param input      an input stream
	 * @param jsonMapper a function that maps a JSON string to a POJO
	 * @param <T>        the type of the POJO
	 * @return the parsed POJO
	 * @throws IOException if an I/O error occurs in the input stream
	 * @since 3.3.0
	 */
	@Nullable
	@CheckReturnValue
	@ApiStatus.Internal
	@ApiStatus.AvailableSince("3.3.0")
	static <T> T fromInputStream(@NotNull InputStreamReader input, @NotNull Function<@NotNull String, @Nullable T> jsonMapper) throws IOException {
		ExceptionUtil.validateNotNull(input, "input");
		ExceptionUtil.validateNotNull(jsonMapper, "jsonMapper");

		StringBuilder sb = new StringBuilder();
		char[] results = new char[1];
		int bytes_read = input.read(results);
		while (results[0] != 0x00 && bytes_read == 1) {
			sb.append(results[0]);
			bytes_read = input.read(results);
		}

		String inJSON = sb.toString();
		// isBlank impl for java 8
		if (inJSON.isEmpty())
			return null;
		boolean eligible = false;
		for (char chr : inJSON.toCharArray()) {
			if (!Character.isWhitespace(chr)) {
				eligible = true;
				break;
			}
		}
		if (!eligible)
			return null;
		// end impl
		return jsonMapper.apply(inJSON);
	}

	/**
	 * Converts this object to its JSON representation.
	 *
	 * @return JSON string
	 * @since 3.3.0
	 */
	@NotNull
	@CheckReturnValue
	@ApiStatus.AvailableSince("3.3.0")
	String toJSON();
}
