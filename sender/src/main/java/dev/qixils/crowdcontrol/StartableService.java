package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.Blocking;

import java.io.IOException;

/**
 * A simulated service that must be manually started.
 */
public interface StartableService extends SimulatedService {
	/**
	 * Starts the simulated server or client.
	 *
	 * @throws IOException if an I/O error occurs in the underlying socket
	 */
	@Blocking
	void connect() throws IOException;
}
