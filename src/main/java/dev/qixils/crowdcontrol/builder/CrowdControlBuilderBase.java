package dev.qixils.crowdcontrol.builder;

import dev.qixils.crowdcontrol.CrowdControl;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.util.Objects;
import java.util.function.Function;

/**
 * A base that implements common methods for building a new {@link CrowdControl} instance.
 */
abstract class CrowdControlBuilderBase implements CrowdControlBuilder {
	/**
	 * A function (usually a constructor) that creates a new {@link SocketManager}
	 * given a {@link CrowdControl} instance.
	 */
	protected final @NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator;

	/**
	 * The port that the client/server will connect to or listen on.
	 */
	protected int port = -1;

	/**
	 * Create a new {@link CrowdControl} using a function that creates a new {@link SocketManager}.
	 *
	 * @param socketManagerCreator creator of a {@link SocketManager} instance
	 */
	@CheckReturnValue
	CrowdControlBuilderBase(@NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator) {
		this.socketManagerCreator = Objects.requireNonNull(socketManagerCreator, "socketManagerCreator cannot be null");
	}

	@Override
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlBuilderBase port(int port) throws IllegalArgumentException {
		if (port < 1 || port > 65536) {
			throw new IllegalArgumentException("Port should be within [1,65536]");
		}
		this.port = port;
		return this;
	}
}
