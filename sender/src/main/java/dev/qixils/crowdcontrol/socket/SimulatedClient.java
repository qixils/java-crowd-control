package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.ServiceManager;
import dev.qixils.crowdcontrol.StartableService;
import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import org.jetbrains.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;

/**
 * A client that connects to a video game hosting a Crowd Control server using the
 * {@code SimpleTCPClientConnector} and dispatches {@link Request}s.
 *
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public final class SimulatedClient implements StartableService<Response>, ServiceManager {
	private static final Logger logger = LoggerFactory.getLogger("CrowdControl/SimulatedClient");
	private final InetAddress ip;
	private final int port;
	private final String password;
	private @Nullable RequestHandler handler = null;
	private boolean running = true;

	/**
	 * Creates a new {@code SimulatedClient} that connects to the given host using the
	 * provided password.
	 *
	 * @param ip       the IP to connect to
	 * @param port     the port to connect to
	 * @param password the password to use
	 * @since 3.9.0
	 */
	@ApiStatus.AvailableSince("3.9.0")
	public SimulatedClient(@NotNull InetAddress ip, int port, @NotNull String password) {
		this.ip = ExceptionUtil.validateNotNull(ip, "ip");
		this.port = port;
		this.password = ServiceManager.encryptPassword(password);
	}

	/**
	 * Creates a new {@code SimulatedClient} that connects to the given host using the
	 * provided password.
	 *
	 * @param ip       the IP to connect to
	 * @param port     the port to connect to
	 * @param password the password to use
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	public SimulatedClient(@NotNull String ip, int port, @NotNull String password) throws UnknownHostException {
		this(InetAddress.getByName(ip), port, password);
	}

	@NotNull
	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public InetAddress getIP() {
		return ip;
	}

	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public int getPort() {
		return port;
	}

	@NotNull
	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public String getPassword() {
		return password;
	}

	@Override
	@Blocking
	@ApiStatus.AvailableSince("3.3.0")
	public void start() throws IOException {
		Socket socket = new Socket(ip, port);
		logger.info("Connected to " + ip + ":" + port);
		handler = new RequestHandler(socket, this, password);
		handler.start();
	}

	@Override
	@CheckReturnValue
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isRunning() {
		return running && handler != null && handler.isRunning();
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isAcceptingRequests() {
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return isRunning() && handler.isAcceptingRequests();
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public boolean isShutdown() {
		return !running;
	}

	@Override
	@ApiStatus.AvailableSince("3.3.0")
	public @NotNull TriState isEffectAvailable(@NotNull String effect) {
		if (handler == null) return TriState.UNKNOWN;
		return handler.isEffectAvailable(ExceptionUtil.validateNotNull(effect, "effect"));
	}

	@Override
	@Blocking
	@ApiStatus.AvailableSince("3.3.0")
	public void shutdown() {
		if (!running) return;
		running = false;
		if (handler != null) handler.shutdown();
	}

	@Override
	@NonBlocking
	@ApiStatus.AvailableSince("3.3.0")
	public @NotNull Flux<@NotNull Response> sendRequest(Request.@NotNull Builder builder, @Nullable Duration timeout) throws IllegalStateException {
		if (!isAcceptingRequests())
			throw new IllegalStateException("Cannot send requests while not accepting requests");
		//noinspection ConstantConditions: call to isRunning() ensures handler is not null
		return handler.sendRequest(builder, timeout);
	}
}
