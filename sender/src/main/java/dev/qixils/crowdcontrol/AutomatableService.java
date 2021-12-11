package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.NonBlocking;
import org.reactivestreams.Publisher;

/**
 * A simulated service that can be automatically (re)started.
 *
 * @param <R> the type of {@link Publisher} returned by the {@code #sendRequest} methods
 */
public interface AutomatableService<R> extends StartableService<R> {
	/**
	 * Starts the simulated server or client and automatically restarts it if it disconnects
	 * (until it is {@link #shutdown() stopped}).
	 */
	@NonBlocking
	void autoStart();
}
