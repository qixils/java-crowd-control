package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.ClientSocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

/**
 * Builds a new {@link CrowdControl} instance that acts as a client
 * and connects to a single Crowd Control server instance.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public final class CrowdControlClientBuilder extends CrowdControlBuilderBase {

	private String IP;

	/**
	 * Creates a new {@link CrowdControl} client builder.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public CrowdControlClientBuilder() {
		super(ClientSocketManager::new);
	}

	/**
	 * Sets the IP that the Crowd Control client will connect to.
	 *
	 * @param IP IP to connect to
	 * @return this builder
	 * @throws IllegalArgumentException the IP was null or blank
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlClientBuilder ip(@NotNull String IP) throws IllegalArgumentException {
		ExceptionUtil.validateNotNull(IP, "IP");
		if (IP.isEmpty()) {
			throw new IllegalArgumentException("IP cannot be blank");
		}
		this.IP = IP;
		return this;
	}

	/**
	 * Sets the port that will be used by the Crowd Control client.
	 *
	 * @param port port to connect to
	 * @return this builder
	 * @throws IllegalArgumentException the port was outside the expected bounds of [1,65536]
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@Override
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlClientBuilder port(int port) throws IllegalArgumentException {
		return (CrowdControlClientBuilder) super.port(port);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return new CrowdControl instance
	 * @throws IllegalStateException {@link #port(int)} or {@link #ip(String)} was not called
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@Override
	@Contract("-> new")
	public @NotNull CrowdControl build() throws IllegalStateException {
		if (port == -1) {
			throw new IllegalStateException("Port must be set using #port(int)");
		}
		if (IP == null) {
			throw new IllegalStateException("IP must be set using #ip(String)");
		}
		return new CrowdControl(IP, port, socketManagerCreator);
	}
}
