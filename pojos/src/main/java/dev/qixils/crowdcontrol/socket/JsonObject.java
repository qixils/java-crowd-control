package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

/**
 * An object that can be serialized into JSON.
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public interface JsonObject {
	/**
	 * Obtains a JSON object from an input stream of 0x00-terminated json strings.
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
	static <T> T fromInputStream(@NotNull InputStream input, @NotNull Function<@NotNull String, @Nullable T> jsonMapper) throws IOException {
		ExceptionUtil.validateNotNull(input, "input");
		ExceptionUtil.validateNotNull(jsonMapper, "jsonMapper");

		// create buffer
		byte[] buffer = new byte[1024];
		int idx = 0;

		while (true) {
			// read next byte
			int readByte = input.read();
			// if we've reached the end of the stream (byte is 0x00 (end of string) or -1 (end of stream)),
			// then truncate the buffer and break
			if (readByte <= 0) {
				buffer = Arrays.copyOf(buffer, idx);
				break;
			}
			// if we've reached the end of the buffer, double it
			if (idx == buffer.length)
				buffer = Arrays.copyOf(buffer, buffer.length * 2);
			// set next byte in buffer
			buffer[idx++] = (byte) readByte;
		}

		// convert bytes to UTF-8 string
		String inJSON = new String(buffer, StandardCharsets.UTF_8);

		// ensure string is not blank
		boolean eligible = false;
		for (char chr : inJSON.toCharArray()) {
			if (!Character.isWhitespace(chr)) {
				eligible = true;
				break;
			}
		}
		if (!eligible)
			return null;

		//LoggerFactory.getLogger("CrowdControl/JsonObject").info(inJSON);

		// convert to POJO
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
