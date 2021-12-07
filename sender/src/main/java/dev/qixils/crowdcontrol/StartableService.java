package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.Blocking;
import org.reactivestreams.Publisher;

import java.io.IOException;

/**
 * A simulated service that must be manually started.
 *
 * @param <R> the type of {@link Publisher} returned by the {@code #sendRequest} methods
 */
public interface StartableService<R> extends SimulatedService<R> {
	/**
	 * Starts the simulated server or client.
	 *
	 * @throws IOException if an I/O error occurs in the underlying socket
	 */
	@Blocking
	void start() throws IOException;
}
