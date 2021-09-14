package dev.qixils.crowdcontrol.socket;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class EnumOrdinalAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	private final T[] values;
	public EnumOrdinalAdapter(Class<T> enumClass) {
		try {
			//noinspection unchecked
			values = (T[]) enumClass.getDeclaredMethod("values").invoke(null);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new IllegalArgumentException("Could not access 'values' method of enum", e);
		}
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}

		out.value(value.ordinal());
	}

	@Override
	public T read(JsonReader in) throws IOException {
		return values[in.nextInt()];
	}
}
