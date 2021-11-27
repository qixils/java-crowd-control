package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import dev.qixils.crowdcontrol.socket.Response.PacketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the connection to Crowd Control clients.
 */
public final class ServerSocketManager implements SocketManager {
    final CrowdControl crowdControl;
    final Executor effectPool = Executors.newCachedThreadPool();
    private final List<SocketThread> socketThreads = new ArrayList<>();
    private ServerSocket serverSocket;
    volatile boolean running = true;
    private static final Logger logger = Logger.getLogger("CC-ServerSocket");

    /**
     * Creates a new server-side socket manager. This is intended only for use by the library.
     * @param crowdControl Crowd Control instance
     */
    @CheckReturnValue
    public ServerSocketManager(@NotNull CrowdControl crowdControl) {
        this.crowdControl = crowdControl;
        new Thread(this::loop, "crowd-control-socket-loop").start();
    }

    private void loop() {
        if (!running)
            return;

        try {
            serverSocket = new ServerSocket(crowdControl.getPort());
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not register port " + crowdControl.getPort() + ". This is a fatal exception; attempts to reconnect will not be made.", exc);
            return;
        }

        while (running) {
            socketThreads.removeIf(SocketThread::isSocketClosed);
            try {
                Socket clientSocket = serverSocket.accept();
                SocketThread socketThread = new SocketThread(this, clientSocket);
                if (!running) {
                    socketThread.shutdown(null, "Server is shutting down");
                    break;
                }
                socketThreads.add(socketThread);
                socketThread.start();
            } catch (IOException exc) {
                if (running)
                    logger.log(Level.WARNING, "Failed to accept new socket connection", exc);
            }
        }
    }

    private String dummyShutdownResponse(@Nullable Request cause, @Nullable String reason) {
        DummyResponse response = new DummyResponse();
        if (cause != null)
            response.id = cause.getId();
        response.message = Objects.requireNonNullElse(reason, "Disconnected");
        response.type = PacketType.DISCONNECT;
        return response.toJSON();
    }

    private void writeResponse(@NotNull String response) {
        for (SocketThread socketThread : socketThreads) {
            if (socketThread.isSocketActive()) {
                try {
                    OutputStream output = socketThread.socket.getOutputStream();
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.write(0x00);
                    output.flush();
                } catch (IOException ignored){}
            }
        }
    }

    @Override
    public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
        writeResponse(dummyShutdownResponse(cause, reason));
        rawShutdown(cause, reason);
    }

    public void rawShutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
        running = false;
        for (SocketThread socketThread : socketThreads) {
            if (socketThread.isSocketActive())
                socketThread.shutdown(cause, reason);
        }
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
    }
}
