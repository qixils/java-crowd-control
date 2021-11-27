package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.socket.Response.PacketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the connection to a Crowd Control client when operating in server mode.
 */
final class SocketThread extends Thread implements SocketManager {
    private static final String RAW_PASSWORD_REQUEST;
    private static final byte[] PASSWORD_REQUEST;
    static {
        DummyResponse resp = new DummyResponse();
        resp.type = PacketType.LOGIN;
        RAW_PASSWORD_REQUEST = resp.toJSON();
        byte[] json = RAW_PASSWORD_REQUEST.getBytes(StandardCharsets.UTF_8);
        // array copy adds an extra 0x00 byte to the end, indicating the end of the packet
        PASSWORD_REQUEST = Arrays.copyOf(json, json.length+1);
    }

    private static final Logger logger = Logger.getLogger("CC-SocketThread");
    final ServerSocketManager socketManager;
    final Socket socket;
    final String displayName = UUID.randomUUID().toString().substring(30).toUpperCase(Locale.ENGLISH);
    private volatile boolean running = true;
    private volatile boolean disconnectMessageSent = false;

    SocketThread(@NotNull ServerSocketManager socketManager, @NotNull Socket clientSocket) {
        this.socketManager = Objects.requireNonNull(socketManager, "socketManager cannot be null");
        this.socket = Objects.requireNonNull(clientSocket, "clientSocket cannot be null");
    }

    public void run() {
        logger.info("Successfully connected to a new client (" + displayName + ")");

        try {
            EffectExecutor effectExecutor = new EffectExecutor(this);

            // prompt client for password
            OutputStream output = socket.getOutputStream();
            output.write(PASSWORD_REQUEST);
            output.flush();

            while (running) {
                effectExecutor.run();
            }

            logger.info("Client socket shutting down (" + displayName + ")");
            writeResponse(dummyShutdownResponse(null, "Server is shutting down"));
        } catch (IOException exc) {
            if ("Connection reset".equals(exc.getMessage())) {
                logger.info("Client disconnected from server (" + displayName + ")");
                return;
            }

            // send disconnection message to socket & ensure socket is closed
            try {
                shutdown(null, running ? "Server encountered an error" : "Server is shutting down");
            } catch (IOException ignored) {}

            // log disconnection
            if (running)
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

    private String dummyShutdownResponse(@Nullable Request cause, @Nullable String reason) {
        DummyResponse response = new DummyResponse();
        if (cause != null)
            response.id = cause.getId();
        response.message = Objects.requireNonNullElse(reason, "Disconnected");
        response.type = PacketType.DISCONNECT;
        return response.toJSON();
    }

    void writeResponse(@NotNull JsonObject response) {
        writeResponse(response.toJSON());
    }

    void writeResponse(@NotNull String response) {
        if (socket.isClosed()) return;
        try {
            OutputStream output = socket.getOutputStream();
            output.write(response.getBytes(StandardCharsets.UTF_8));
            output.write(0x00);
            output.flush();
        } catch (IOException ignored){}
    }

    @Override
    public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
        if (!disconnectMessageSent) {
            disconnectMessageSent = true;
            writeResponse(dummyShutdownResponse(cause, reason));
        }
        rawShutdown();
    }

    private void rawShutdown() throws IOException {
        running = false;
        if (!socket.isClosed())
            socket.close();
    }
}
