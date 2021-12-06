package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.NonBlocking;

/**
 * A simulated service that can be automatically (re)started.
 */
public interface AutomatableService extends StartableService {
	/**
	 * Starts the simulated server or client and automatically restarts it if it disconnects
	 * (until it is {@link #shutdown() stopped}).
	 */
	@NonBlocking
	void start();
}
