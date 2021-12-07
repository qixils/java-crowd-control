package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.builder.CrowdControlClientBuilder;
import dev.qixils.crowdcontrol.builder.CrowdControlServerBuilder;
import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.exceptions.NoApplicableTarget;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API for interacting with <a href="https://crowdcontrol.live">Crowd Control</a> via the
 * {@code SimpleTCPConnector} or {@code SimpleTCPClientConnector}.
 * <h2>Creating an instance</h2>
 * <p>
 * To create an instance of this class, use either the {@link #client()} or {@link #server()}
 * methods.
 * </p>
 * <ul>
 *     <li>
 *         {@link #client()} creates a client instance that can be used to connect to a central
 *         Crowd Control server. It corresponds with the {@code SimpleTCPConnector} in your
 *         project's .cs file. In this mode, your project will only be able to connect to one
 *         streamer, although multiple instances of your project will all be able to connect to
 *         this streamer at the same time. This mode is ideal for single-player games.
 *     </li>
 *     <li>
 *         {@link #server()} creates a server instance that can be used to connect to multiple
 *         Crowd Control clients. It corresponds with the {@code SimpleTCPClientConnector}.
 *         This allows you to receive requests from multiple streamers, making this ideal for
 *         multiplayer games.
 *     </li>
 * </ul>
 * <p>
 * You should only ever create one instance of this class. If you must recreate this instance,
 * remember to call {@link #shutdown(String)} first.
 * </p>
 * <h2>Registering effect handlers</h2>
 * <p>
 * Effect handlers process incoming effect {@link Request}s, apply them to players or environments
 * as appropriate, and then return a {@link Response} that informs the client of whether the effect
 * was successfully applied.
 * </p>
 * <p>
 * To start handling incoming effects, you much register handlers for each effect
 * you want to handle using one of {@link #registerHandler(String, Consumer)},
 * {@link #registerHandler(String, Function)}, or {@link #registerHandlers(Object)}.
 * </p>
 * <h2>Registering checks</h2>
 * <p>
 * Checks allow you to block an effect from being processed if certain conditions are not met.
 * This is typically used to ensure effects are not run before the game has loaded into a world.
 * You can register checks using {@link #registerCheck(Supplier)}
 * or {@link #registerCheck(Function)}.
 * </p>
 */
public final class CrowdControl implements SocketManager, RequestManager {

	private static final Logger logger = Logger.getLogger("CC-Core");
	private static final Map<Class<?>, Function<Object, Response>> RETURN_TYPE_PARSERS = Map.of(
			Response.class, response -> (Response) response,
			Response.Builder.class, builder -> ((Response.Builder) builder).build()
	);
	private final Map<String, Function<Request, Response>> effectHandlers = new HashMap<>();
	private final Map<String, Consumer<Request>> asyncHandlers = new HashMap<>();
	private final List<Function<Request, CheckResult>> globalChecks = new ArrayList<>();
	private final @Nullable String IP;
	private final int port;
	private final @Nullable String password;
	private final SocketManager socketManager;

	/**
	 * Creates a new Crowd Control client or server instance.
	 * <p>
	 * You should generally be using {@link #server()} or {@link #client()} instead.
	 * This constructor's parameters are prone to changes.
	 *
	 * @param IP                   IP address to connect to (if applicable)
	 * @param port                 port to listen on or connect to
	 * @param password             password required to connect (if applicable)
	 * @param socketManagerCreator creator of a new {@link SocketManager}
	 */
	public CrowdControl(@Nullable String IP,
						int port,
						@Nullable String password,
						@NotNull Function<@NotNull CrowdControl, @NotNull SocketManager> socketManagerCreator) {
		this.IP = IP;
		this.port = port;
		this.socketManager = socketManagerCreator.apply(this);
		this.password = password == null
				? null
				: ServiceManager.encryptPassword(password);
	}

	/**
	 * Returns a builder for a new {@link CrowdControl} instance operating in client mode.
	 * It will connect to a singular Crowd Control server instance.
	 *
	 * @return a new client builder
	 */
	public static CrowdControlClientBuilder client() {
		return new CrowdControlClientBuilder();
	}

	/**
	 * Returns a builder for a new {@link CrowdControl} instance operating in server mode.
	 * It will allow numerous Crowd Control clients to connect.
	 *
	 * @return a new server builder
	 */
	public static CrowdControlServerBuilder server() {
		return new CrowdControlServerBuilder();
	}

	/**
	 * Renders a warning for improperly configured {@link Subscribe} methods.
	 *
	 * @param method           improperly configured method
	 * @param errorDescription issue with the method
	 */
	private static void methodHandlerWarning(@NotNull Method method, @NotNull String errorDescription) {
		logger.warning("Method " + method.getName() + " is improperly configured: " + errorDescription);
	}

	/**
	 * Returns the IP that the {@link SocketManager} will listen on.
	 * If running in server mode, this will be null.
	 *
	 * @return IP if available
	 */
	@Nullable
	@CheckReturnValue
	public String getIP() {
		return IP;
	}

	/**
	 * Returns the port that the {@link SocketManager} will listen on.
	 *
	 * @return IP port
	 */
	@CheckReturnValue
	public int getPort() {
		return port;
	}

	/**
	 * Returns the password required for clients to connect to this server as a SHA-512 encrypted,
	 * Base64-encoded string. If running in client mode, this will be null.
	 *
	 * @return password required to connect
	 */
	@Nullable
	@CheckReturnValue
	public String getPassword() {
		return password;
	}

	/**
	 * Registers method handlers within a class. These methods must:
	 * <ul>
	 *     <li>be public</li>
	 *     <li>have the {@link Subscribe} annotation with a non-null effect name</li>
	 *     <li>only one parameter, which has the type {@link Request}</li>
	 *     <li>a return type of one of the following:<ul>
	 *         <li>{@link Response}</li>
	 *         <li>{@link Response.Builder}</li>
	 *         <li>Void (assumes you will call {@link Response#send()} yourself)</li>
	 *     </ul></li>
	 * </ul>
	 *
	 * @param object class instance to register
	 */
	public void registerHandlers(@NotNull Object object) {
		Class<?> clazz = Objects.requireNonNull(object, "object").getClass();
		for (Method method : clazz.getMethods()) {
			if (!method.isAnnotationPresent(Subscribe.class)) continue;
			String nullableEffect = method.getAnnotation(Subscribe.class).effect();
			//noinspection ConstantConditions
			if (nullableEffect == null) {
				methodHandlerWarning(method, "effect name is null");
				continue;
			}

			final String effect = nullableEffect.toLowerCase(Locale.ENGLISH);
			if (effectHandlers.containsKey(effect) || asyncHandlers.containsKey(effect)) {
				methodHandlerWarning(method, "handler by the name '" + effect + "' is already registered");
				continue;
			}

			if (!Modifier.isPublic(method.getModifiers())) {
				methodHandlerWarning(method, "should be public");
				continue;
			}

			Parameter[] params = method.getParameters();
			if (params.length != 1) {
				methodHandlerWarning(method, "expected 1 input parameter, received " + params.length);
				continue;
			}

			Class<?> paramType = params[0].getType();
			if (!Request.class.equals(paramType)) {
				methodHandlerWarning(method, "expected input parameter of type Request, received " + paramType.getName());
				continue;
			}

			Class<?> returnType = method.getReturnType();
			if (RETURN_TYPE_PARSERS.containsKey(returnType)) {
				Function<Object, Response> parser = RETURN_TYPE_PARSERS.get(returnType);
				registerHandler(effect, request -> {
					Response output;
					try {
						Object result = method.invoke(object, request);
						output = result == null
								? request.buildResponse().type(Response.ResultType.FAILURE).message("Effect handler returned a null response").build()
								: parser.apply(result);
					} catch (IllegalAccessException | InvocationTargetException e) {
						logger.log(Level.WARNING, "Failed to invoke method handler for effect \"" + effect + "\"", e);
						output = request.buildResponse().type(Response.ResultType.FAILURE).message("Failed to invoke method handler").build();
					}
					return output;
				});
			} else if (returnType.equals(Void.TYPE)) {
				registerHandler(effect, request -> {
					try {
						method.invoke(object, request);
					} catch (IllegalAccessException | InvocationTargetException e) {
						logger.log(Level.WARNING, "Failed to invoke method handler for effect \"" + effect + "\"", e);
						request.buildResponse().type(Response.ResultType.FAILURE).message("Failed to invoke method handler").send();
					}
				});
			} else {
				methodHandlerWarning(method, "unknown return type: " + returnType.getName());
			}
		}
	}

	/**
	 * Registers a function to handle an effect.
	 *
	 * @param effect  name of the effect to handle
	 * @param handler function to handle the effect
	 * @see #registerHandler(String, Consumer)
	 */
	public void registerHandler(@NotNull String effect, @NotNull Function<Request, Response> handler) {
		effect = Objects.requireNonNull(effect, "effect").toLowerCase(Locale.ENGLISH);
		if (effectHandlers.containsKey(effect) || asyncHandlers.containsKey(effect)) {
			throw new IllegalArgumentException("The effect \"" + effect + "\" already has a handler.");
		}
		effectHandlers.put(effect, Objects.requireNonNull(handler, "handler"));
	}

	/**
	 * Registers an effect handler which does not immediately return a {@link Response}.
	 * It is expected to call {@link Response#send()} on its own.
	 *
	 * @param effect  name of the effect to handle
	 * @param handler function to handle the effect
	 * @see #registerHandler(String, Function)
	 */
	public void registerHandler(@NotNull String effect, @NotNull Consumer<Request> handler) {
		effect = Objects.requireNonNull(effect, "effect").toLowerCase(Locale.ENGLISH);
		if (effectHandlers.containsKey(effect) || asyncHandlers.containsKey(effect)) {
			throw new IllegalArgumentException("The effect \"" + effect + "\" already has a handler.");
		}
		asyncHandlers.put(effect, Objects.requireNonNull(handler, "handler"));
	}

	/**
	 * Registers a check which will be called for every incoming {@link Request}.
	 * A resulting value of {@link CheckResult#DISALLOW} will result in an {@link Response.ResultType#FAILURE FAILURE} response packet.
	 * <p>
	 * This is used for validating that your service is accepting requests, and should return {@link CheckResult#DISALLOW}
	 * if, for example, the game has not fully initialized or no players are connected.
	 *
	 * @param check global check to register
	 */
	public void registerCheck(@NotNull Function<Request, CheckResult> check) {
		globalChecks.add(Objects.requireNonNull(check, "check"));
	}

	/**
	 * Registers a check which will be called for every incoming {@link Request}.
	 * A resulting value of {@link CheckResult#DISALLOW} will result in an {@link Response.ResultType#FAILURE FAILURE} response packet.
	 * <p>
	 * This is used for validating that your service is accepting requests, and should return {@link CheckResult#DISALLOW}
	 * if, for example, the game has not fully initialized or no players are connected.
	 *
	 * @param check global check to register
	 */
	public void registerCheck(@NotNull Supplier<CheckResult> check) {
		Objects.requireNonNull(check, "check");
		globalChecks.add($ -> check.get());
	}

	/**
	 * Handles an incoming {@link Request} by executing the relevant handler.
	 *
	 * @param request an incoming request
	 */
	public void handle(@NotNull Request request) {
		for (Function<Request, CheckResult> check : globalChecks) {
			if (check.apply(request) == CheckResult.DISALLOW) {
				request.buildResponse().type(Response.ResultType.FAILURE).message("The game is unavailable").send();
			}
		}

		String effect = Objects.requireNonNull(request, "request").getEffect();

		try {
			if (effectHandlers.containsKey(effect))
				effectHandlers.get(effect).apply(request).send();
			else if (asyncHandlers.containsKey(effect))
				asyncHandlers.get(effect).accept(request);
			else
				request.buildResponse().type(Response.ResultType.UNAVAILABLE).message("The effect couldn't be found").send();
		} catch (Exception e) {
			if (ExceptionUtil.isCause(NoApplicableTarget.class, e)) {
				request.buildResponse().type(Response.ResultType.FAILURE).message("Streamer(s) unavailable").send();
			} else {
				logger.log(Level.WARNING, "Failed to handle effect \"" + effect + "\"", e);
				request.buildResponse().type(Response.ResultType.FAILURE).message("The effect encountered an exception").send();
			}
		}
	}

	/**
	 * Shuts down the internal connection to the Crowd Control server.
	 *
	 * @see #shutdown(String)
	 * @see #shutdown(Request, String)
	 * @deprecated providing error messages via {@link #shutdown(String)} is recommended
	 */
	@SuppressWarnings("deprecation")
	// yes, I know that I am overriding a deprecated method, deal with it
	@Deprecated
	public void shutdown() {
		try {
			socketManager.shutdown(null, null);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Encountered an exception while shutting down socket", e);
		}
	}

	/**
	 * Shuts down the internal connection to the Crowd Control server and
	 * sends a corresponding error message to the streamer(s).
	 *
	 * @param reason the reason for shutting down
	 */
	public void shutdown(@Nullable String reason) {
		try {
			socketManager.shutdown(null, reason);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Encountered an exception while shutting down socket", e);
		}
	}

	/**
	 * Shuts down the internal connection to the Crowd Control server and
	 * sends a corresponding error message to the streamer(s).
	 *
	 * @param cause  cause for shutting down
	 * @param reason the reason for shutting down
	 */
	public void shutdown(@Nullable Request cause, @Nullable String reason) {
		try {
			socketManager.shutdown(cause, reason);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Encountered an exception while shutting down socket", e);
		}
	}

}