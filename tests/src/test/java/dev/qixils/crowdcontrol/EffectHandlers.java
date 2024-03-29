package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

// doubles as a test for all the possible handlers available to #registerEffects!
@SuppressWarnings("unused") // used by reflection
public final class EffectHandlers {
	private final @NotNull Function<@NotNull TimedEffect, Response.@Nullable Builder> timedEffectCallback;
	private final @Nullable Consumer<@NotNull TimedEffect> completionCallback;

	EffectHandlers(@Nullable Function<@NotNull TimedEffect, Response.@Nullable Builder> timedEffectCallback, @Nullable Consumer<@NotNull TimedEffect> completionCallback) {
		this.timedEffectCallback = ExceptionUtil.validateNotNullElse(timedEffectCallback, $ -> new Response.Builder());
		this.completionCallback = completionCallback;
	}

	EffectHandlers(@Nullable Function<@NotNull TimedEffect, Response.@Nullable Builder> timedEffectCallback) {
		this(timedEffectCallback, null);
	}

	EffectHandlers() {
		this(null, null);
	}

	@Subscribe("success")
	public Response success(Request request) {
		return request.buildResponse().type(Response.ResultType.SUCCESS).build();
	}

	@Subscribe("failure")
	public Response.Builder failure(Request request) {
		return request.buildResponse().type(Response.ResultType.FAILURE);
	}

	@Subscribe("retry")
	public void retry(Request request) {
		request.buildResponse().type(Response.ResultType.RETRY).send();
	}

	@Subscribe("timed")
	public void timed(Request request) {
		new TimedEffect.Builder()
				.request(request)
				.duration(200)
				.startCallback(timedEffectCallback)
				.build().queue();
	}

	@Subscribe("nothing")
	public void nothing(Request request) {
		// this should time out
	}

	@Subscribe("timedEffectError")
	public void timedEffectError(Request request) {
		new TimedEffect.Builder()
				.request(request)
				.duration(Duration.ofMillis(30))
				.startCallback($ -> {
					throw new RuntimeException("This is an error");
				})
				.completionCallback(timedEffectCallback::apply)
				.build().queue();
	}

	@Subscribe("timedEffectRetry")
	public void timedEffectRetry(Request request) {
		new TimedEffect.Builder()
				.request(request)
				.duration(Duration.ofMillis(30))
				.startCallback($ -> request.buildResponse().type(Response.ResultType.RETRY))
				.completionCallback(timedEffectCallback::apply)
				.build().queue();
	}

	@Subscribe("timedEffectCompletionCallback")
	public void timedEffectCompletionCallback(Request request) {
		TimedEffect effect = new TimedEffect.Builder()
				.request(request)
				.duration(Duration.ofMillis(75))
				.startCallback(timedEffectCallback)
				.completionCallback(completionCallback)
				.build();
		effect.queue();
	}
}
