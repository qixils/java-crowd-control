package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.CrowdControl;
import dev.qixils.crowdcontrol.exceptions.NoApplicableTarget;
import dev.qixils.crowdcontrol.socket.Request.Type;
import dev.qixils.crowdcontrol.socket.Response.PacketType;
import dev.qixils.crowdcontrol.socket.Response.ResultType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes incoming requests from a Crowd Control socket and executes them.
 */
final class EffectExecutor {
	private static final Logger logger = Logger.getLogger("CC-EffectExecutor");
	private final @Nullable SocketThread socketThread;
	private final Socket socket;
	private final Executor effectPool;
	private final InputStreamReader input;
	private final CrowdControl crowdControl;
	private final @Nullable String password;
	private boolean loggedIn = false;

	EffectExecutor(SocketThread socketThread) throws IOException {
		this.socketThread = socketThread;
		this.socket = socketThread.socket;
		this.effectPool = socketThread.socketManager.effectPool;
		this.input = new InputStreamReader(socket.getInputStream());
		this.crowdControl = socketThread.socketManager.crowdControl;
		this.password = crowdControl.getPassword();
	}

	EffectExecutor(Socket socket, Executor effectPool, CrowdControl crowdControl) throws IOException {
		this.socketThread = null;
		this.socket = socket;
		this.effectPool = effectPool;
		this.input = new InputStreamReader(socket.getInputStream());
		this.crowdControl = crowdControl;
		this.password = crowdControl.getPassword();
	}

	void run() throws IOException {
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
			request.originatingSocket = socket;
		} catch (Exception exc) {
			logger.log(Level.SEVERE, "Could not parse request " + inJSON, exc);
			return;
		}

		if (request.getType() == Type.KEEP_ALIVE) {
			request.buildResponse().packetType(PacketType.KEEP_ALIVE).send();
			return;
		}

		// login handling
		if (!loggedIn && password != null && socketThread != null) {
			if (request.getType() != Type.LOGIN) {
				request.buildResponse().type(ResultType.NOT_READY).message("Client has not logged in").send();
			} else if (password.equals(request.getMessage())) {
				loggedIn = true;
			} else {
				socketThread.shutdown();
			}
			return;
		}

		// process request
		effectPool.execute(() -> {
			try {
				crowdControl.handle(request);
			} catch (Throwable exc) {
				if (CrowdControl.isCause(NoApplicableTarget.class, exc)) {
					request.buildResponse().type(ResultType.FAILURE).message("Streamer(s) unavailable").send();
				} else {
					logger.log(Level.WARNING, "Request handler threw an exception", exc);
					request.buildResponse().type(ResultType.FAILURE).message("Request handler threw an exception").send();
				}
			}
		});
	}
}
