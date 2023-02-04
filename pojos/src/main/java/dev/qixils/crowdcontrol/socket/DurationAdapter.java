package dev.qixils.crowdcontrol.socket;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;

/**
 * GSON Adapter for {@link Duration} to/from JSON.
 */
class DurationAdapter extends TypeAdapter<Duration> {

	/**
	 * Constructs a new {@link DurationAdapter}.
	 */
	public DurationAdapter() {
	}

	@Override
	public @Nullable Duration read(@NotNull JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return Duration.ofMillis(in.nextLong());
	}

	@Override
	public void write(@NotNull JsonWriter out, @Nullable Duration value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.toMillis());
		}
	}
}
