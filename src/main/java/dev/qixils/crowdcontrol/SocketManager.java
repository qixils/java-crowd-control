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

	SocketManager(CrowdControl crowdControl) {
		this.crowdControl = crowdControl;
		thread.start();
	}

	private void loop() {
		while (running) {
			try {
				socket = new Socket(crowdControl.getIP(), crowdControl.getPort());
				logger.info("Connected to Crowd Control");
				InputStreamReader input = new InputStreamReader(socket.getInputStream()); // TODO: might need to go in while loop
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
					Request request = Request.fromJSON(sb.toString());

					// process request
					Response.Result result = crowdControl.handle(request);
					Response response = new Response(request.getId(), result);
					String outJSON = response.toJSON() + (byte) 0x00; // TODO: ensure this functions
					output.write(outJSON.getBytes(StandardCharsets.UTF_8));
				}

				logger.info("Crowd Control socket shutting down");
			} catch (IOException e) {
				logger.log(Level.WARNING, "Socket loop encountered an error", e);
			}
		}
	}

	public void shutdown() throws IOException {
		running = false;
		if (socket != null)
			socket.close();
	}
}
