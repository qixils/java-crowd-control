package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

// this file is not as well documented because I don't consider it "API" :P

/**
 * Manages the connection to the Crowd Control server
 */
final class SocketManager {
	private final CrowdControl crowdControl;
	private final Thread thread = new Thread(this::loop, "crowdcontrol-socket-loop");
	private Socket socket;
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
				OutputStream output = socket.getOutputStream();

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
					try {
						Response.Result result = crowdControl.handle(request);
						Response response = new Response(request.getId(), result);
						output.write(response.toJSON().getBytes(StandardCharsets.UTF_8));
						output.write(0x00);
					} catch (Exception e) {
						logger.log(Level.WARNING, "Request handler threw an exception", e);
					}
				}

				logger.info("Crowd Control socket shutting down");
			} catch (IOException e) {
				if (!running)
					continue;

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

	/**
	 * Shuts down the Crowd Control server socket.
	 * @throws IOException an I/O exception occurred while trying to close the socket
	 */
	public void shutdown() throws IOException {
		running = false;
		if (socket != null)
			socket.close();
	}
}
