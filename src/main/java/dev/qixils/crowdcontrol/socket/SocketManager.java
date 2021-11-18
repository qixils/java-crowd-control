package dev.qixils.crowdcontrol.socket;

import java.io.IOException;

/**
 * Manages the connection(s) to a Crowd Control server or clients
 */
public interface SocketManager {
    /**
     * Shuts down the Crowd Control socket.
     * @throws IOException an I/O exception occurred while trying to close the socket
     */
    void shutdown() throws IOException;
}
