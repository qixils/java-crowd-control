package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

/**
 * Builds a new {@link CrowdControl} instance.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
@ApiStatus.Internal
interface CrowdControlBuilder {
	/**
	 * Sets the port that will be used by the Crowd Control client or server.
	 *
	 * @param port port to listen on
	 * @return this builder
	 * @throws IllegalArgumentException the port was outside the expected bounds of [1,65536]
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	@NotNull
	@Contract("_ -> this")
	CrowdControlBuilder port(int port) throws IllegalArgumentException;

	/**
	 * Builds a new {@link CrowdControl} instance using the provided variables.
	 *
	 * @return new CrowdControl instance
	 * @throws IllegalStateException {@link #port(int)} was not called
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	@NotNull
	@Contract("-> new")
	CrowdControl build() throws IllegalStateException;
}
