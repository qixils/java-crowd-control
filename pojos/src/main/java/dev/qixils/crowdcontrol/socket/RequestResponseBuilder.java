package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

class RequestResponseBuilder extends Response.Builder {
	private final @NotNull Request request;

	RequestResponseBuilder(@NotNull Request request) {
		super(request);
		this.request = request;
	}

	@Override
	public @NotNull Response build() {
		Response.ResultType result = type();
		if (result != Response.ResultType.UNAVAILABLE)
			return super.build();

		// append the unavailable effect as a suffix to the message
		final String originalMessage = ExceptionUtil.validateNotNullElseGet(message(), result::name);
		message(originalMessage + " [effect: " + request.getEffect() + "]");

		// build the response
		Response built = super.build();

		// undo the message changes
		message(originalMessage);
		return built;
	}
}
