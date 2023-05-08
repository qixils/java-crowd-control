package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Manages the connection(s) to a Crowd Control server or clients.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public interface SocketManager extends Respondable {

	/**
	 * Creates a {@link Response} {@link Response.Builder builder} which will be dispatched to
	 * the connected server or clients upon calling {@link Response#send()}.
	 * <p>
	 * Unlike the usual methods of creating a {@link Response}, this does not require a
	 * corresponding {@link Request} to send the packet.
	 *
	 * @return a new response builder
	 * @since 3.5.2
	 */
	@ApiStatus.AvailableSince("3.5.2")
	Response.@NotNull Builder buildResponse();

	/**
	 * Shuts down the Crowd Control socket.
	 *
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 * @see #shutdown(String)
	 * @see #shutdown(Request, String)
	 * @since 3.0.0
	 * @deprecated providing error messages via {@link #shutdown(String)} is recommended
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

	/**
	 * Sets a consumer to be called when a new {@link SocketManager Crowd Control instance}
	 * connects.
	 *
	 * @param consumer consumer to be called
	 * @since 3.4.0
	 */
	@ApiStatus.AvailableSince("3.4.0")
	void addConnectListener(@NotNull Consumer<SocketManager> consumer); // TODO: unit tests

	/**
	 * Gets the {@link Request.Source}s that this {@link SocketManager} is connected to.
	 * The returned collection is unmodifiable.
	 *
	 * @return the sources that this socket manager is connected to
	 * @since 3.6.0
	 */
	@ApiStatus.AvailableSince("3.6.0")
	@NotNull Collection<Request.Source> getSources();
}
