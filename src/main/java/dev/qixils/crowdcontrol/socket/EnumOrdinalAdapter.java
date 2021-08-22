package dev.qixils.crowdcontrol.socket;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class EnumOrdinalAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	private final Class<T> enumClass;
	public EnumOrdinalAdapter(Class<T> enumClass) {
		this.enumClass = enumClass;
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
		try {
			return ((T[]) enumClass.getDeclaredMethod("values").invoke(null))[in.nextInt()];
		} catch (Exception e) {
			throw new IOException("Could not read enum value", e);
		}
	}
}
