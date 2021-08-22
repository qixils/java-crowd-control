package dev.qixils.crowdcontrol.socket;

import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public class EnumOrdinalAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null || !value.getClass().isEnum()) {
			out.nullValue();
			return;
		}

		out.value(value.ordinal());
	}

	@Override
	public T read(JsonReader in) throws IOException {
		Type type = new TypeToken<T>(){}.getType(); // magic??
		try {
			return ((T[]) type.getClass().getMethod("values").invoke(null))[in.nextInt()];
		} catch (Exception e) {
			throw new IOException("Could not read enum value", e);
		}
	}
}
