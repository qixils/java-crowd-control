package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import java.io.IOException;

/**
 * A simulated service that must be manually started.
 *
 * @param <R> the type of {@link Publisher} returned by the {@code #sendRequest} methods
 */
public interface StartableService<R extends Publisher<@NotNull Response>> extends SimulatedService<R> {
	/**
	 * Starts the simulated server or client.
	 *
	 * @throws IOException if an I/O error occurs in the underlying socket
	 */
	@Blocking
	void start() throws IOException;
}
