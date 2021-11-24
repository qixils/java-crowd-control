package dev.qixils.crowdcontrol.socket;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the connection to a Crowd Control client when operating in server mode.
 */
final class SocketThread extends Thread implements SocketManager {
    private static final Logger logger = Logger.getLogger("CC-SocketThread");
    final ServerSocketManager socketManager;
    final Socket socket;
    final String displayName = UUID.randomUUID().toString().substring(30).toUpperCase(Locale.ENGLISH);
    private volatile boolean running = true;

    SocketThread(@NotNull ServerSocketManager socketManager, @NotNull Socket clientSocket) {
        this.socketManager = Objects.requireNonNull(socketManager, "socketManager cannot be null");
        this.socket = Objects.requireNonNull(clientSocket, "clientSocket cannot be null");
    }

    public void run() {
        logger.info("Successfully connected to a new client (" + displayName + ")");

        try {
            EffectExecutor effectExecutor = new EffectExecutor(this);

            // prompt client for password
            String passwordType = String.valueOf(Response.PacketType.LOGIN.getEncodedByte());
            String passwordRequest = "{\"id\":0,\"type\":" + passwordType + "}";
            OutputStream output = socket.getOutputStream();
            output.write(passwordRequest.getBytes(StandardCharsets.UTF_8));
            output.write(0x00);
            output.flush();

            while (running) {
                effectExecutor.run();
            }

            logger.info("Client socket shutting down (" + displayName + ")");
        } catch (IOException exc) {
            // ensure socket is closed
            if (!socket.isClosed())
                try {socket.close();} catch (IOException ignored) {}

            // log disconnection
            if ("Connection reset".equals(exc.getMessage()))
                logger.info("Client disconnected from server (" + displayName + ")");
            else if (running)
                logger.log(Level.WARNING, "Erroneously disconnected from client socket (" + displayName + ")", exc);
            else
                logger.info("Client socket shutting down (" + displayName + ")");
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
