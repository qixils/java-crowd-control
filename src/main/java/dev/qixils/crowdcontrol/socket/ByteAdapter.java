package dev.qixils.crowdcontrol.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;

class ByteAdapter<T> extends TypeAdapter<T> {
	static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Request.Type.class, new ByteAdapter<>(Request.Type::from, Request.Type::getEncodedByte))
			.registerTypeAdapter(Response.ResultType.class, new ByteAdapter<>(Response.ResultType::from, Response.ResultType::getEncodedByte))
			.create();

	private final @NotNull Function<@NotNull Byte, @Nullable T> fromByte;
	private final @NotNull Function<@NotNull T, @NotNull Byte> toByte;

	public ByteAdapter(@NotNull Function<@NotNull Byte, @NotNull T> fromByte,
					   @NotNull Function<@NotNull T, @NotNull Byte> toByte) {
		this.fromByte = fromByte;
		this.toByte = toByte;
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(toByte.apply(value));
		}
	}

	@Override
	public T read(JsonReader in) throws IOException {
		return fromByte.apply((byte) in.nextInt());
	}
}
