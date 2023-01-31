package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Something that can be {@link Response responded} to.
 */
public interface Respondable {

	/**
	 * Creates a {@link Response} {@link Response.Builder builder}.
	 *
	 * @return a new response builder
	 * @since 3.5.2
	 */
	@ApiStatus.AvailableSince("3.5.2")
	Response.@NotNull Builder buildResponse();
}
