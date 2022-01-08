package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.reactivestreams.Publisher;

import java.io.IOException;

/**
 * A simulated service that must be manually started.
 *
 * @param <R> the type of {@link Publisher} returned by the {@code #sendRequest} methods
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public interface StartableService<R> extends SimulatedService<R> {
	/**
	 * Starts the simulated server or client.
	 *
	 * @throws IOException if an I/O error occurs in the underlying socket
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@Blocking
	void start() throws IOException;
}
