package dev.qixils.crowdcontrol.builder;

import dev.qixils.crowdcontrol.CrowdControl;
import dev.qixils.crowdcontrol.socket.ServerSocketManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

/**
 * Builds a new {@link CrowdControl} instance that acts as a server for Crowd Control clients to connect to.
 */
public final class CrowdControlServerBuilder extends CrowdControlBuilderBase {
	private String password;

	/**
	 * Creates a new {@link CrowdControl} server builder.
	 */
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
	 */
	@CheckReturnValue
	@Contract("_ -> this")
	public @NotNull CrowdControlServerBuilder password(@NotNull String password) throws IllegalArgumentException {
		//noinspection ConstantConditions
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("password must be non-null and not blank");
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
	 */
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
	 */
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
		return new CrowdControl(null, port, password, socketManagerCreator);
	}
}
