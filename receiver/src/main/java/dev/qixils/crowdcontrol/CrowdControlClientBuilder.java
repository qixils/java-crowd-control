package dev.qixils.crowdcontrol;

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
public final class CrowdControlClientBuilder extends CrowdControlBuilderBase<CrowdControlClientBuilder> {

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
