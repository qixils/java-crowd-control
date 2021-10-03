package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// this file is not as well documented because I don't consider it "API" :P

/**
 * Manages the connection to the Crowd Control server
 */
final class SocketManager {
	private final CrowdControl crowdControl;
	private final Thread thread = new Thread(this::loop, "crowdcontrol-socket-loop");
	private final Executor effectPool = Executors.newCachedThreadPool();
	private Socket socket;
	private OutputStream output;
	private volatile boolean running = true;
	private static final Logger logger = Logger.getLogger("CC-Socket");
	private int sleep = 1;
	private boolean connected = false;

	SocketManager(CrowdControl crowdControl) {
		this.crowdControl = crowdControl;
		thread.start();
	}

	private void loop() {
		while (running) {
			try {
				socket = new Socket(crowdControl.getIP(), crowdControl.getPort());
				logger.info("Connected to Crowd Control server");
				sleep = 1;
				connected = true;
				InputStreamReader input = new InputStreamReader(socket.getInputStream());
				output = socket.getOutputStream();

				while (running) {
					// get incoming data
					StringBuilder sb = new StringBuilder();
					char[] results = new char[1];
					int bytes_read = input.read(results);
					while (results[0] != 0x00 && bytes_read == 1) {
						sb.append(results[0]);
						bytes_read = input.read(results);
					}
					String inJSON = sb.toString();
					Request request;
					try {
						request = Request.fromJSON(inJSON);
					} catch (Exception exc) {
						logger.log(Level.SEVERE, "Could not parse request " + inJSON, exc);
						break;
					}

					// process request
					effectPool.execute(() -> {
						try {
							crowdControl.handle(request);
						} catch (Exception e) {
							logger.log(Level.WARNING, "Request handler threw an exception", e);
							sendResponse(Response.builder().type(Response.ResultType.FAILURE).message("Request handler threw an exception").build());
						}
					});
				}

				logger.info("Crowd Control socket shutting down");
			} catch (IOException e) {
				if (!running)
					continue;

				socket = null;
				output = null;
				String error = connected ? "Socket loop encountered an error" : "Could not connect to the Crowd Control server";
				Throwable exc = connected ? e : null;
				logger.log(Level.WARNING, error + ". Reconnecting in " + sleep + "s", exc);
				try {
					Thread.sleep(sleep * 1000L);
				} catch (InterruptedException ignored) {}
				sleep *= 2;
			}
		}
	}

	boolean acceptingResponses() {
		return output != null;
	}

	synchronized void sendResponse(@NotNull Response response) {
		Objects.requireNonNull(response, "response cannot be null");
		if (output == null)
			throw new IllegalStateException("Socket output is unavailable");

		try {
			output.write(response.toJSON().getBytes(StandardCharsets.UTF_8));
			output.write(0x00);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to write response to socket");
		}
	}

	/**
	 * Shuts down the Crowd Control server socket.
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 */
	void shutdown() throws IOException {
		running = false;
		if (socket != null)
			socket.close();
	}
}
