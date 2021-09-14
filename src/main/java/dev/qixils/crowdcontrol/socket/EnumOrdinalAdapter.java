package dev.qixils.crowdcontrol.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

final class EnumOrdinalAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Request.Type.class, new EnumOrdinalAdapter<>(Request.Type.class))
			.registerTypeAdapter(Response.ResultType.class, new EnumOrdinalAdapter<>(Response.ResultType.class))
			.create();

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
