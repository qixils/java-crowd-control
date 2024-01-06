package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;

/**
 * A base that implements common methods for building a new {@link CrowdControl} instance.
 *
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
@ApiStatus.AvailableSince("3.0.0")
abstract class CrowdControlBuilderBase<B extends CrowdControlBuilderBase<B>> implements CrowdControlBuilder<B> {

	protected final Logger logger = LoggerFactory.getLogger("CrowdControl/Builder");

	/**
	 * A function (usually a constructor) that creates a new {@link SocketManager}
	 * given a {@link CrowdControl} instance.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	protected final @NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator;

	/**
	 * The IP that the client/server will connect to or bind on.
	 *
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	protected InetAddress IP;

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
	public @NotNull B port(int port) throws IllegalArgumentException {
		if (port < 1 || port > 65536) {
			throw new IllegalArgumentException("Port should be within [1,65536]");
		}
		this.port = port;
		return (B) this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param IP IP to connect to
	 * @return this builder
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull B ip(@Nullable InetAddress IP) throws IllegalArgumentException {
		this.IP = IP;
		return (B) this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param IP IP to connect to
	 * @return this builder
	 * @throws IllegalArgumentException the IP was invalid
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull B ip(@Nullable String IP) throws IllegalArgumentException {
		if (IP == null || IP.isEmpty()) {
			logger.warn("Received null IP; ignoring");
			this.IP = null;
			return (B) this;
		}
		try {
			this.IP = InetAddress.getByName(IP);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IP address " + IP, e);
		}
		return (B) this;
	}
}
