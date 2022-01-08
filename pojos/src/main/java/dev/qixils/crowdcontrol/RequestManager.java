package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A class that holds variables for a {@link SocketManager} and can process incoming data from
 * the socket.
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public interface RequestManager extends ServiceManager {
	/**
	 * Handles an incoming request by dispatching it to the appropriate effect handler.
	 *
	 * @param request the request to handle
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	void handle(@NotNull Request request);
}
