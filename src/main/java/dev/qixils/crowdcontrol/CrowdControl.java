package dev.qixils.crowdcontrol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qixils.crowdcontrol.socket.EnumOrdinalAdapter;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
	private final Map<String, Function<Request, Response.Result>> effectHandlers = new HashMap<>();
	private final List<Supplier<Boolean>> globalChecks = new ArrayList<>();
	private final String IP;
	private final int port;
	private final SocketManager socketManager;
	private static final Logger logger = Logger.getLogger("CC-Core");
	public static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Request.Type.class, new EnumOrdinalAdapter<>(Request.Type.class))
			.registerTypeAdapter(Response.ResultType.class, new EnumOrdinalAdapter<>(Response.ResultType.class))
			.create();

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

	/**
	 * Registers all methods inside of a class with:
	 * <ul>
	 *     <li>the {@link Subscribe} annotation</li>
	 *     <li>one parameter of type {@link Request}</li>
	 *     <li>the return type {@link Response.ResultType}</li>
	 * </ul>
	 * @param object class instance to register
	 */
	public void registerHandlers(@NotNull Object object) {
		Class<?> clazz = Objects.requireNonNull(object, "object").getClass();
		for (Method method : clazz.getMethods()) {
			if (Modifier.isPublic(method.getModifiers()) // ensure method is public
					&& (method.getReturnType() == Response.ResultType.class || method.getReturnType() == Response.Result.class) // validate return type
					&& method.getParameterCount() == 1 && method.getParameters()[0].getType() == Request.class // validate parameter
					&& method.isAnnotationPresent(Subscribe.class)) { // validate it is an event handler
				String effect = method.getAnnotation(Subscribe.class).effect();
				registerHandler(effect, request -> {
					Response.Result output;
					try {
						Object result = method.invoke(object, request);
						if (result instanceof Response.ResultType res) {
							output = new Response.Result(res);
						} else if (result instanceof Response.Result wrap) {
							output = wrap;
						} else {
							output = new Response.Result(Response.ResultType.UNAVAILABLE, "Unknown result type");
						}
					} catch (IllegalAccessException | InvocationTargetException e) {
						logger.log( Level.WARNING,"Failed to invoke method handler for effect \"" + effect + "\"", e);
						output = new Response.Result(Response.ResultType.FAILURE, "Failed to invoke method handler");
					}
					return output;
				});
			}
		}
	}

	/**
	 * Registers a function to handle an effect.
	 * @param effect name of the effect to handle
	 * @param handler function to handle the effect
	 */
	public void registerHandler(@NotNull String effect, @NotNull Function<Request, Response.Result> handler) {
		effect = Objects.requireNonNull(effect, "effect").toLowerCase(Locale.ENGLISH);
		if (effectHandlers.containsKey(effect)) {
			throw new IllegalArgumentException("The effect \"" + effect + "\" already has a handler.");
		}
		effectHandlers.put(effect, Objects.requireNonNull(handler, "handler"));
	}

	/**
	 * Registers a check which will be called for every incoming {@link Request}.
	 * A resulting value of {@code false} will result in an {@link Response.ResultType#UNAVAILABLE UNAVAILABLE} response packet.
	 * <p>
	 * This is used for validating that your service is accepting requests, and should return false if,
	 * for example, the game has not fully initialized or no players are connected.
	 */
	public void registerCheck(@NotNull Supplier<Boolean> check) {
		globalChecks.add(Objects.requireNonNull(check, "check"));
	}

	/**
	 * Handles an incoming {@link Request} by executing the relevant handler.
	 * @param request an incoming request
	 * @return the result of handling the request
	 */
	@NotNull
	public Response.Result handle(@NotNull Request request) {
		for (Supplier<Boolean> check : globalChecks) {
			if (!check.get()) {
				return new Response.Result(Response.ResultType.UNAVAILABLE, "The game is unavailable");
			}
		}

		String effect = Objects.requireNonNull(request, "request").getEffect();
		if (!effectHandlers.containsKey(effect)) {
			return new Response.Result(Response.ResultType.UNAVAILABLE, "The effect couldn't be found");
		}

		try {
			return effectHandlers.get(effect).apply(request);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to handle effect \"" + effect + "\"", e);
			return new Response.Result(Response.ResultType.FAILURE, "The effect encountered an exception");
		}
	}

	/**
	 * Shuts down the internal connection to the Crowd Control server.
	 */
	public void shutdown() {
		try {
			socketManager.shutdown();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Encounted an exception while shutting down socket", e);
		}
	}

}
