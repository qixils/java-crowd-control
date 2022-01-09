package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonBlocking;
import org.reactivestreams.Publisher;

/**
 * A simulated service that can be automatically (re)started.
 *
 * @param <R> the type of {@link Publisher} returned by the {@code #sendRequest} methods
 * @since 3.3.0
 * @deprecated non-functional; scheduled for removal in 3.4.0
 */
@ApiStatus.AvailableSince("3.3.0")
@ApiStatus.ScheduledForRemoval(inVersion = "3.4.0")
@Deprecated
public interface AutomatableService<R> extends StartableService<R> {
	/**
	 * Starts the simulated server or client and automatically restarts it if it disconnects
	 * (until it is {@link #shutdown() stopped}).
	 *
	 * <p>This method does not function properly and thus will be removed in v3.4.0.</p>
	 *
	 * @throws IllegalStateException if the service is already running or has been shut down
	 * @since 3.3.0
	 * @deprecated non-functional; scheduled for removal in 3.4.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@ApiStatus.ScheduledForRemoval(inVersion = "3.4.0")
	@Deprecated
	@NonBlocking
	void autoStart();
}
