package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

// doubles as a test for all the possible handlers available to #registerEffects!
@SuppressWarnings("unused") // used by reflection
public class EffectHandlers {
	private final @NotNull Consumer<@NotNull TimedEffect> timedEffectCallback;

	EffectHandlers(@Nullable Consumer<@NotNull TimedEffect> timedEffectCallback) {
		this.timedEffectCallback = ExceptionUtil.validateNotNullElse(timedEffectCallback, $ -> {
		});
	}

	EffectHandlers() {
		this(null);
	}

	@Subscribe(effect = "success")
	public Response success(Request request) {
		return request.buildResponse().type(Response.ResultType.SUCCESS).build();
	}

	@Subscribe(effect = "failure")
	public Response.Builder failure(Request request) {
		return request.buildResponse().type(Response.ResultType.FAILURE);
	}

	@Subscribe(effect = "retry")
	public void retry(Request request) {
		request.buildResponse().type(Response.ResultType.RETRY).send();
	}

	@Subscribe(effect = "timed")
	public void timed(Request request) {
		new TimedEffect(request, 200, timedEffectCallback, $ -> {
		}).queue();
	}

	@Subscribe(effect = "nothing")
	public void nothing(Request request) {
		// this should time out
	}
}
