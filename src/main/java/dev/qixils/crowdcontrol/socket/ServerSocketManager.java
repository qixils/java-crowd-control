package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
        }

        while (running) {
            socketThreads.removeIf(SocketThread::isSocketClosed);
            try {
                Socket clientSocket = serverSocket.accept();
                if (!running) {
                    clientSocket.close();
                    return;
                }
                SocketThread socketThread = new SocketThread(this, clientSocket);
                socketThreads.add(socketThread);
                socketThread.start();
            } catch (IOException exc) {
                logger.log(Level.WARNING, "Failed to accept new socket connection", exc);
            }
        }
    }

    @Override
    public void shutdown() throws IOException {
        running = false;
        for (SocketThread socketThread : socketThreads) {
            if (socketThread.isSocketActive())
                socketThread.shutdown();
        }
    }
}
