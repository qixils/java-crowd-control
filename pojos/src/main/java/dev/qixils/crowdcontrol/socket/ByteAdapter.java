package dev.qixils.crowdcontrol.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;

class ByteAdapter<T extends ByteObject> extends TypeAdapter<T> {
	static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Request.Type.class, new ByteAdapter<>(Request.Type::from))
			.registerTypeAdapter(Response.ResultType.class, new ByteAdapter<>(Response.ResultType::from))
			.registerTypeAdapter(Response.PacketType.class, new ByteAdapter<>(Response.PacketType::from))
			.create();

	private final @NotNull Function<@NotNull Byte, @Nullable T> fromByte;

	public ByteAdapter(@NotNull Function<@NotNull Byte, @NotNull T> fromByte) {
		this.fromByte = ExceptionUtil.validateNotNull(fromByte, "fromByte");
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.getEncodedByte());
		}
	}

	@Override
	public T read(JsonReader in) throws IOException {
		return fromByte.apply((byte) in.nextInt());
	}
}
