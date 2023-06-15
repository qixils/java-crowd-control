package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages the connection(s) to a Crowd Control server or clients.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public interface SocketManager extends Respondable {

	/**
	 * Gets a unique display name for this {@link SocketManager}.
	 *
	 * @return a unique display name
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	@NotNull String getDisplayName();

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
	@NotNull Set<Request.Source> getSources();

	/**
	 * Gets the {@link Request.Source} that this {@link SocketManager} is connected to
	 * if there is only one. If there are zero or more than one, {@code null} is returned.
	 *
	 * @return the source that this socket manager is connected to, or {@code null}
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	default Request.@Nullable Source getSource() {
		Set<Request.Source> sources = getSources();
		if (sources.size() == 1)
			return sources.iterator().next();
		return null;
	}

	/**
	 * Returns an unmodifiable collection of the individual connected {@link SocketManager}s represented by this
	 * {@link SocketManager}. If this {@link SocketManager} is standalone, a list containing only this instance is
	 * returned.
	 *
	 * @return the active socket managers
	 * @since 3.6.1
	 */
	@ApiStatus.AvailableSince("3.6.1")
	default @NotNull List<? extends SocketManager> getConnections() {
		return Collections.singletonList(this);
	}

	/**
	 * Determines whether this socket is closed.
	 *
	 * @return {@code true} if this socket is closed, {@code false} otherwise
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	boolean isClosed();

	/**
	 * Writes a {@link Response} to the connected server or clients.
	 *
	 * @param response the response to write
	 * @throws IOException an I/O exception occurred while trying to write the response
	 * @since 3.7.0
	 */
	@ApiStatus.AvailableSince("3.7.0")
	void write(@NotNull Response response) throws IOException;
}
