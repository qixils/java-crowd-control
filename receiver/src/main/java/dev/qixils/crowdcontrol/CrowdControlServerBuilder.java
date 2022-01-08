package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.ServerSocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

/**
 * Builds a new {@link CrowdControl} instance that acts as
 * a server for Crowd Control clients to connect to.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public final class CrowdControlServerBuilder extends CrowdControlBuilderBase {
	private String password;

	/**
	 * Creates a new {@link CrowdControl} server builder.
	 *
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	public CrowdControlServerBuilder() {
		super(ServerSocketManager::new);
	}

	/**
	 * Sets the password required for Crowd Control clients to connect.
	 *
	 * @param password password clients must enter to connect
	 * @return this builder
	 * @throws IllegalArgumentException the password was null or blank
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlServerBuilder password(@NotNull String password) throws IllegalArgumentException {
		ExceptionUtil.validateNotNull(password, "password");
		if (password.isEmpty()) {
			throw new IllegalArgumentException("password cannot be blank");
		}
		this.password = password;
		return this;
	}

	/**
	 * Sets the port that will be used by the Crowd Control server.
	 *
	 * @param port port to listen for new connections on
	 * @return this builder
	 * @throws IllegalArgumentException the port was outside the expected bounds of [1,65536]
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@Override
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlServerBuilder port(int port) throws IllegalArgumentException {
		return (CrowdControlServerBuilder) super.port(port);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return new CrowdControl instance
	 * @throws IllegalStateException {@link #port(int)} or {@link #password(String)} was not called
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@Override
	@CheckReturnValue
	@Contract("-> new")
	public @NotNull CrowdControl build() throws IllegalStateException {
		if (port == -1) {
			throw new IllegalStateException("Port must be set using #port(int)");
		}
		if (password == null) {
			throw new IllegalStateException("Password must be set using #password(String)");
		}
		return new CrowdControl(port, password, socketManagerCreator);
	}
}
