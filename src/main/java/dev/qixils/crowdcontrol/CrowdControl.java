package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;

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
 * API for interacting with <a href="https://crowdcontrol.live">Crowd Control</a> via the SimpleTCPConnector.
 * <p>
 * You should only ever create one instance of this class.
 */
public class CrowdControl {
	private final Map<String, Function<Request, Response>> effectHandlers = new HashMap<>();
	private final Map<String, Consumer<Request>> asyncHandlers = new HashMap<>();
	private final List<Supplier<Boolean>> globalChecks = new ArrayList<>();
	private final String IP;
	private final int port;
	private final SocketManager socketManager;
	private static final Logger logger = Logger.getLogger("CC-Core");

	/**
	 * Creates a CrowdControl API instance which listens to the local server.
	 * @param port local port to listen on
	 */
	public CrowdControl(int port) {
		this("localhost", port);
	}

	/**
	 * Creates a CrowdControl API instance which listens to a server.
	 * @param IP IP to listen on
	 * @param port port to listen on
	 */
	public CrowdControl(@NotNull String IP, int port) {
		this.IP = Objects.requireNonNull(IP, "IP");
		this.port = port;
		socketManager = new SocketManager(this);
	}

	/**
	 * Returns the IP that the {@link SocketManager} will listen on.
	 * @return IP
	 */
	@NotNull
	public String getIP() {
		return IP;
	}

	/**
	 * Returns the port that the {@link SocketManager} will listen on.
	 * @return IP port
	 */
	public int getPort() {
		return port;
	}

	private static final Map<Class<?>, Function<Object, Response>> RETURN_TYPE_PARSERS = Map.of(
			Response.class, response -> (Response) response,
			Response.ResultType.class, type -> Response.builder().type((Response.ResultType) type).build(),
			Response.Builder.class, builder -> ((Response.Builder) builder).build()
	);

	/**
	 * Renders a warning for improperly configured {@link Subscribe} methods.
	 * @param method improperly configured method
	 * @param errorDescription issue with the method
	 */
	private void methodHandlerWarning(@NotNull Method method, @NotNull String errorDescription) {
		logger.warning("Method " + method + " is improperly configured: " + errorDescription);
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
	 *         <li>{@link Response.ResultType}</li>
	 *     </ul></li>
	 * </ul>
	 * @param object class instance to register
	 */
	public void registerHandlers(@NotNull Object object) {
		Class<?> clazz = Objects.requireNonNull(object, "object").getClass();
		for (Method method : clazz.getMethods()) {
			if (!method.isAnnotationPresent(Subscribe.class)) continue;
			String nullableEffect = method.getAnnotation(Subscribe.class).effect();
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
								? Response.builder().type(Response.ResultType.FAILURE).message("Effect handler returned a null response").build()
								: parser.apply(result);
					} catch (IllegalAccessException | InvocationTargetException e) {
						logger.log(Level.WARNING, "Failed to invoke method handler for effect \"" + effect + "\"", e);
						output = Response.builder().type(Response.ResultType.FAILURE).message("Failed to invoke method handler").build();
					}
					return output;
				});
			} else if (returnType == Void.class) {
				registerHandler(effect, request -> {
					try {
						method.invoke(object, request);
					} catch (IllegalAccessException | InvocationTargetException e) {
						logger.log(Level.WARNING, "Failed to invoke method handler for effect \"" + effect + "\"", e);
						dispatchResponse(Response.builder().type(Response.ResultType.FAILURE).message("Failed to invoke method handler").build());
					}
				});
			} else {
				methodHandlerWarning(method, "unknown return type: " + returnType.getName());
			}
		}
	}

	/**
	 * Registers a function to handle an effect.
	 * @param effect name of the effect to handle
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
	 * It is expected to call {@link #dispatchResponse(Response)} on its own.
	 * @param effect name of the effect to handle
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
	 * A resulting value of {@code false} will result in an {@link Response.ResultType#UNAVAILABLE UNAVAILABLE} response packet.
	 * <p>
	 * This is used for validating that your service is accepting requests, and should return {@code false} if,
	 * for example, the game has not fully initialized or no players are connected.
	 * @param check global check to register
	 */
	public void registerCheck(@NotNull Supplier<Boolean> check) {
		globalChecks.add(Objects.requireNonNull(check, "check"));
	}

	/**
	 * Handles an incoming {@link Request} by executing the relevant handler.
	 * @param request an incoming request
	 */
	public void handle(@NotNull Request request) {
		for (Supplier<Boolean> check : globalChecks) {
			if (!check.get()) {
				dispatchResponse(Response.builder().type(Response.ResultType.FAILURE).message("The game is unavailable").build());
			}
		}

		String effect = Objects.requireNonNull(request, "request").getEffect();

		try {
			if (effectHandlers.containsKey(effect))
				dispatchResponse(effectHandlers.get(effect).apply(request));
			else if (asyncHandlers.containsKey(effect))
				asyncHandlers.get(effect).accept(request);
			else
				dispatchResponse(Response.builder().type(Response.ResultType.UNAVAILABLE).message("The effect couldn't be found").build());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to handle effect \"" + effect + "\"", e);
			dispatchResponse(Response.builder().type(Response.ResultType.FAILURE).message("The effect encountered an exception").build());
		}
	}

	/**
	 * Sends a {@link Response} to the Crowd Control server.
	 * @param response effect response
	 */
	public void dispatchResponse(@NotNull Response response) {
		socketManager.sendResponse(response);
	}

	/**
	 * Sends a {@link Response} to the Crowd Control server.
	 * @param response effect response
	 */
	public void dispatchResponse(Response.@NotNull Builder response) {
		dispatchResponse(Objects.requireNonNull(response, "response cannot be null").build());
	}

	/**
	 * Shuts down the internal connection to the Crowd Control server.
	 */
	public void shutdown() {
		try {
			socketManager.shutdown();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Encountered an exception while shutting down socket", e);
		}
	}

}
