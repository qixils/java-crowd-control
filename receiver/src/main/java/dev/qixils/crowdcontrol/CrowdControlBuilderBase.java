package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.util.function.Function;

/**
 * A base that implements common methods for building a new {@link CrowdControl} instance.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
@ApiStatus.Internal
abstract class CrowdControlBuilderBase implements CrowdControlBuilder {
	/**
	 * A function (usually a constructor) that creates a new {@link SocketManager}
	 * given a {@link CrowdControl} instance.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	protected final @NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator;

	/**
	 * The port that the client/server will connect to or listen on.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	protected int port = -1;

	/**
	 * Create a new {@link CrowdControl} using a function that creates a new {@link SocketManager}.
	 *
	 * @param socketManagerCreator creator of a {@link SocketManager} instance
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	CrowdControlBuilderBase(@NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator) {
		this.socketManagerCreator = ExceptionUtil.validateNotNull(socketManagerCreator, "socketManagerCreator");
	}

	@Override
	@CheckReturnValue
	@Contract("_ -> this")
	@ApiStatus.AvailableSince("3.0.0")
	public @NotNull CrowdControlBuilderBase port(int port) throws IllegalArgumentException {
		if (port < 1 || port > 65536) {
			throw new IllegalArgumentException("Port should be within [1,65536]");
		}
		this.port = port;
		return this;
	}
}
