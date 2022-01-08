package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Manages the connection(s) to a Crowd Control server or clients.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public interface SocketManager {
	/**
	 * Shuts down the Crowd Control socket.
	 *
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 * @see #shutdown(String)
	 * @see #shutdown(Request, String)
	 * @deprecated providing error messages via {@link #shutdown(String)} is recommended
	 * @since 3.0.0
	 */
	@Deprecated
	@ApiStatus.AvailableSince("3.0.0")
	default void shutdown() throws IOException {
		shutdown(null);
	}

	/**
	 * Shuts down the Crowd Control socket and sends an explanation message to the streamer.
	 *
	 * @param reason the reason for shutting down
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 * @since 3.1.0
	 */
	@ApiStatus.AvailableSince("3.1.0")
	default void shutdown(@Nullable String reason) throws IOException {
		shutdown(null, reason);
	}

	/**
	 * Shuts down the Crowd Control socket and sends an explanation message to the streamer.
	 *
	 * @param cause  cause for shutting down
	 * @param reason the reason for shutting down
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 * @since 3.1.0
	 */
	@ApiStatus.AvailableSince("3.1.0")
	void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException;
}
