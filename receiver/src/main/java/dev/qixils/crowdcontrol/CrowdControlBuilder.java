package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.net.InetAddress;

/**
 * Builds a new {@link CrowdControl} instance.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
@ApiStatus.Internal
interface CrowdControlBuilder<B extends CrowdControlBuilder<B>> {

	/**
	 * Sets the IP that the Crowd Control client will connect to.
	 *
	 * @param IP IP to connect to
	 * @return this builder
	 * @throws IllegalArgumentException the IP was null or blank
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	@CheckReturnValue
	@Contract("_ -> this")
	@NotNull
	B ip(@NotNull InetAddress IP);

	/**
	 * Sets the IP that the Crowd Control client will connect to.
	 *
	 * @param IP IP to connect to
	 * @return this builder
	 * @throws IllegalArgumentException the IP was null or blank
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	@CheckReturnValue
	@Contract("_ -> this")
	@NotNull
	B ip(@NotNull String IP) throws IllegalArgumentException;

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
	B port(int port) throws IllegalArgumentException;

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
