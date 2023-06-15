package dev.qixils.crowdcontrol.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.util.PostProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

class ByteAdapter<T extends ByteObject> extends TypeAdapter<T> {
	static final @NotNull Gson GSON = new GsonBuilder()
			.registerTypeAdapterFactory(new PostProcessor())
			.registerTypeAdapter(Request.Type.class, new ByteAdapter<>(Request.Type::from))
			.registerTypeAdapter(Response.ResultType.class, new ByteAdapter<>(Response.ResultType::from))
			.registerTypeAdapter(Response.PacketType.class, new ByteAdapter<>(Response.PacketType::from))
			.registerTypeAdapter(IdType.class, new ByteAdapter<>(IdType::from))
			.registerTypeAdapter(Duration.class, new DurationAdapter())
			.create();

	private final @NotNull Function<@NotNull Byte, @Nullable T> fromByte;

	public ByteAdapter(@NotNull Function<@NotNull Byte, @NotNull T> fromByte) {
		this.fromByte = ExceptionUtil.validateNotNull(fromByte, "fromByte");
	}

	@Override
	public void write(@Nullable JsonWriter out, @Nullable T value) throws IOException {
		if (out == null) return;

		if (value == null) {
			out.nullValue();
		} else {
			long ub = value.getEncodedByte() & 0xFF;
			out.value(ub);
		}
	}

	@Override
	public @Nullable T read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return fromByte.apply((byte) in.nextInt());
	}
}
