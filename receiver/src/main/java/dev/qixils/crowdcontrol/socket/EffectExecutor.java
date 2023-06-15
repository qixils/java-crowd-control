package dev.qixils.crowdcontrol.socket;

import com.google.gson.JsonParseException;
import dev.qixils.crowdcontrol.RequestManager;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.exceptions.NoApplicableTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * Processes incoming requests from a Crowd Control socket and executes them.
 */
final class EffectExecutor {
	private static final Logger logger = LoggerFactory.getLogger("CC-EffectExecutor");
	private final @Nullable SocketManager socketThread;
	private final Socket socket;
	private final Executor effectPool;
	private final InputStream input;
	private final RequestManager crowdControl;
	private final @Nullable String password;
	private boolean loggedIn = false;
	private Request.@Nullable Source player = null;

	EffectExecutor(SocketThread socketThread) throws IOException {
		this.socketThread = socketThread;
		this.socket = socketThread.socket;
		this.effectPool = socketThread.socketManager.effectPool;
		this.input = socket.getInputStream();
		this.crowdControl = socketThread.socketManager.crowdControl;
		this.password = crowdControl.getPassword();
	}

	EffectExecutor(ClientSocketManager csm) throws IOException {
		this.socketThread = csm;
		this.socket = csm.socket;
		if (this.socket == null)
			throw new IOException("Socket is null");
		this.effectPool = csm.effectPool;
		this.input = socket.getInputStream();
		this.crowdControl = csm.crowdControl;
		this.password = crowdControl.getPassword();
	}

	Request.@NotNull Source getSource() {
		if (player == null)
			player = new Request.Source.Builder().ip(socket.getInetAddress()).build();
		return player;
	}

	void run() throws IOException {
		// get incoming data
		Request request;
		try {
			request = JsonObject.fromInputStream(input, Request::fromJSON);
		} catch (JsonParseException e) {
			logger.error("Failed to parse JSON from socket", e);
			return;
		}

		if (request == null) {
			logger.debug("Received a blank packet; assuming client has disconnected");
			try {
				if (socketThread != null)
					socketThread.shutdown("Received a blank packet; assuming client has disconnected");
				else
					socket.close();
			} catch (IOException e) {
				logger.debug("Ignoring exception thrown by socket; likely just a result of the socket terminating");
			}
			return;
		}

		request.setOriginatingSocket(socketThread);

		if (request.getType() == Request.Type.PLAYER_INFO) {
			Request.Source.Builder source = getSource().toBuilder();
			if (request.getPlayer() != null)
				source.target(request.getPlayer());
			else if (request.getTargets().length == 1)
				source.target(request.getTargets()[0]);
			player = source.build();
		} else if (player != null) {
			request.setSource(getSource());
		}

		if (request.getType() == Request.Type.KEEP_ALIVE) {
			request.buildResponse().packetType(Response.PacketType.KEEP_ALIVE).send();
			return;
		}

		// login handling
		if (!loggedIn && password != null && socketThread != null) {
			if (request.getType() != Request.Type.LOGIN) {
				request.buildResponse().type(Response.ResultType.NOT_READY).message("Client has not logged in").send();
			} else if (password.equalsIgnoreCase(request.getPassword())) {
				logger.info("New client successfully logged in (" + socketThread.getDisplayName() + ")");
				request.buildResponse().packetType(Response.PacketType.LOGIN_SUCCESS).message("Successfully logged in").send();
				player = getSource().toBuilder().login(request.getLogin()).build();
				loggedIn = true;
			} else {
				logger.info("Aborting connection due to incorrect password (" + socketThread.getDisplayName() + ")");
				socketThread.shutdown(request, "Incorrect password");
			}
			return;
		}

		// process request
		effectPool.execute(() -> {
			try {
				crowdControl.handle(request);
			} catch (Throwable exc) {
				if (ExceptionUtil.isCause(NoApplicableTarget.class, exc)) {
					request.buildResponse().type(Response.ResultType.FAILURE).message("Streamer(s) unavailable").send();
				} else {
					logger.error("Request handler threw an exception", exc);
					request.buildResponse().type(Response.ResultType.FAILURE).message("Request handler threw an exception").send();
				}
			}
		});
	}

	boolean isClosed() {
		return socket.isClosed() || !socket.isConnected() || socket.isOutputShutdown();
	}

	void write(@NotNull Response response) throws IOException {
		String json = response.toJSON();
		logger.debug("Sending response to client: " + json);
		synchronized (socket) {
			OutputStream output = socket.getOutputStream();
			output.write(json.getBytes(StandardCharsets.UTF_8));
			output.write(0x00);
			output.flush();
		}
	}
}
