package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Handles the connection to a Crowd Control client when operating in server mode.
 */
final class SocketThread extends Thread implements SocketManager {
    private static final Logger logger = Logger.getLogger("CC-SocketThread");
    final ServerSocketManager socketManager;
    final Socket socket;
    private volatile boolean running;

    SocketThread(@NotNull ServerSocketManager socketManager, @NotNull Socket clientSocket) {
        this.socketManager = Objects.requireNonNull(socketManager, "socketManager cannot be null");
        this.socket = Objects.requireNonNull(clientSocket, "clientSocket cannot be null");
    }

    public void run() {
        try {
            EffectExecutor effectExecutor = new EffectExecutor(this);

            while (running) {
                effectExecutor.run();
            }

            logger.fine("Client socket shutting down");
        } catch (IOException exc) {
            // ensure socket is closed
            if (!socket.isClosed())
                try {socket.close();} catch (IOException ignored) {}

            // log disconnection
            if (running)
                logger.info("Disconnected from client socket");
            else
                logger.fine("Client socket shutting down");
        }
    }

    public boolean isSocketActive() {
        return running && !socket.isClosed();
    }

    public boolean isSocketClosed() {
        return !isSocketActive();
    }

    @Override
    public void shutdown() throws IOException {
        running = false;
        if (!socket.isClosed())
            socket.close();
    }
}
