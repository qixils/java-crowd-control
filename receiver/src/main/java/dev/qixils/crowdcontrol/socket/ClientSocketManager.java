package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.RequestManager;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Manages the connection to the Crowd Control server.
 *
 * @since 3.0.0
 */
@ApiStatus.AvailableSince("3.0.0")
public final class ClientSocketManager implements SocketManager {
	private static final @NotNull Logger logger = LoggerFactory.getLogger("CrowdControl/ClientSocket");
	final @NotNull RequestManager crowdControl;
	final @NotNull Executor effectPool = Executors.newCachedThreadPool();
	private final @NotNull List<Consumer<SocketManager>> onConnectListeners = new ArrayList<>();
	final @NotNull List<Consumer<SocketManager>> onLoginListeners = new ArrayList<>();
	@Nullable Socket socket;
	private volatile boolean running = true;
	private int sleep = 1;
	private boolean connected = false;
	private EffectExecutor effectExecutor;

	/**
	 * Creates a new client-side socket manager. This is intended only for use by the library.
	 *
	 * @param serverConfig Crowd Control instance
	 * @since 3.0.0
	 */
	@CheckReturnValue
	@ApiStatus.Internal
	@ApiStatus.AvailableSince("3.0.0")
	public ClientSocketManager(@NotNull RequestManager serverConfig) {
		this.crowdControl = ExceptionUtil.validateNotNull(serverConfig, "serverConfig");
		new Thread(this::loop, "crowd-control-socket-loop").start();
	}

	@Override
	public void addConnectListener(@NotNull Consumer<SocketManager> consumer) {
		onConnectListeners.add(ExceptionUtil.validateNotNull(consumer, "consumer"));
	}

	@Override
	public void addLoginListener(@NotNull Consumer<SocketManager> consumer) {
		onLoginListeners.add(ExceptionUtil.validateNotNull(consumer, "consumer"));
	}

	@Override
	public Response.@NotNull Builder buildResponse() {
		return new Response.Builder().originatingSocket(this);
	}

	private void loop() {
		while (running) {
			try {
				socket = new Socket(crowdControl.getIP(), crowdControl.getPort());
				logger.info("Connected to Crowd Control server");
				for (Consumer<SocketManager> listener : onConnectListeners) {
					try {
						listener.accept(this);
					} catch (Throwable t) {
						logger.warn("Error while calling connect listener", t);
					}
				}
				sleep = 1;
				connected = true;
				effectExecutor = new EffectExecutor(
						this
				);

				while (running) {
					effectExecutor.run();
				}

				logger.info("Crowd Control socket shutting down");
				Response.ofDisconnectMessage(this, "Server is shutting down").send();
			} catch (IOException e) {
				if ("Connection reset".equals(e.getMessage())) {
					logger.info("Server terminated connection");
				} else if (socket != null && !socket.isClosed()) {
					// send error message
					Response.ofDisconnectMessage(this, running ? "Server encountered an error" : "Server is shutting down").send();

					// ensure socket is closed
					try {
						socket.close();
					} catch (IOException ignored) {
					}
				}

				if (!running)
					continue;

				// render error message with exponential decay
				socket = null;
				String error = connected ? "Socket loop encountered an error" : "Could not connect to the Crowd Control server";
				Throwable exc = connected ? e : null;
				logger.warn(error + ". Reconnecting in " + sleep + "s", exc);
				try {
					Thread.sleep(sleep * 1000L);
				} catch (InterruptedException ignored) {
					if (!running) return;
				}
				sleep *= 2;
			}
		}
	}

	@Override
	@ApiStatus.AvailableSince("3.1.0")
	public void shutdown(@Nullable Request cause, @Nullable String reason) throws IOException {
		if (!running) return;
		running = false;
		if (socket != null && !socket.isClosed()) {
			Response.ofDisconnectMessage(this, reason).send();
			socket.close();
		}
	}

	@Override
	public @NotNull Set<Request.Source> getSources() {
		if (effectExecutor == null)
			return Collections.emptySet();
		return Collections.singleton(effectExecutor.getSource());
	}

	@Override
	public Request.@Nullable Source getSource() {
		return effectExecutor == null ? null : effectExecutor.getSource();
	}

	@Override
	public boolean isClosed() {
		return !running;
	}

	@Override
	public void write(@NotNull Response response) throws IOException {
		if (effectExecutor == null)
			throw new IOException("Socket is not connected");
		effectExecutor.write(response);
	}

	@Override
	public @NotNull String getDisplayName() {
		return "Client";
	}
}
