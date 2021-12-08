package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.TriState;
import dev.qixils.crowdcontrol.exceptions.EffectUnavailableException;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.time.Duration;

/**
 * An object used to simulate a Crowd Control server or client that sends
 * {@link dev.qixils.crowdcontrol.socket.Request effect requests} to a connected video game.
 *
 * @param <R> the type of response returned by the {@code #sendRequest} methods
 */
public interface SimulatedService<R> {
	/**
	 * Time until a request {@link java.util.concurrent.TimeoutException times out}.
	 */
	Duration TIMEOUT = Duration.ofSeconds(15);

	/**
	 * Determines if the simulated service is currently running.
	 *
	 * @return {@code true} if the simulated service is currently running
	 */
	@CheckReturnValue
	@NonBlocking
	boolean isRunning();

	/**
	 * Determines if the simulated service is currently accepting requests.
	 *
	 * @return {@code true} if the simulated service is currently accepting requests
	 */
	@CheckReturnValue
	@NonBlocking
	default boolean isAcceptingRequests() {
		return isRunning();
	}

	/**
	 * Determines if the simulated service was {@link #shutdown() shutdown}.
	 *
	 * @return {@code true} if the simulated service was {@link #shutdown() shutdown}
	 */
	@CheckReturnValue
	@NonBlocking
	boolean isShutdown();

	/**
	 * Stops the simulated server or client.
	 */
	@Blocking
	void shutdown();

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param request the {@link Request} to dispatch
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if no response is received in a {@link #TIMEOUT timely manner},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 * @throws IllegalArgumentException   if {@code request} is {@code null} or invalid
	 * @throws EffectUnavailableException the provided effect is known to be {@link #isEffectAvailable(String) unavailable}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(@NotNull Request request) throws IllegalArgumentException, EffectUnavailableException {
		return sendRequest(request, true);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param builder the {@link Response.Builder} to dispatch
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if no response is received in a {@link #TIMEOUT timely manner},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 * @throws IllegalArgumentException   if {@code builder} is {@code null} or invalid
	 * @throws EffectUnavailableException the provided effect is known to be {@link #isEffectAvailable(String) unavailable}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(Request.@NotNull Builder builder) throws IllegalArgumentException, EffectUnavailableException {
		return sendRequest(builder, true);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param request the {@link Request} to dispatch
	 * @param timeout whether the returned Flux should throw a {@link java.util.concurrent.TimeoutException}
	 *                if no response is received in a {@link #TIMEOUT timely manner}
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if {@code timeout} is {@code true}
	 * and no response is received in a {@link #TIMEOUT timely manner},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 * @throws IllegalArgumentException   if {@code request} is {@code null} or invalid
	 * @throws EffectUnavailableException the provided effect is known to be {@link #isEffectAvailable(String) unavailable}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(@NotNull Request request, boolean timeout) throws IllegalArgumentException, EffectUnavailableException {
		return sendRequest(request.toBuilder(), timeout);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param builder the {@link Response.Builder} to dispatch
	 * @param timeout whether the returned Flux should throw a {@link java.util.concurrent.TimeoutException}
	 *                if no response is received in a {@link #TIMEOUT timely manner}
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if {@code timeout} is {@code true}
	 * and no response is received in a {@link #TIMEOUT timely manner},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 * @throws IllegalArgumentException   if {@code builder} is {@code null} or invalid
	 * @throws EffectUnavailableException the provided effect is known to be {@link #isEffectAvailable(String) unavailable}
	 */
	@NotNull
	@NonBlocking
	Flux<@NotNull R> sendRequest(Request.@NotNull Builder builder, boolean timeout) throws IllegalArgumentException, EffectUnavailableException;

	/**
	 * Determines if an effect by that name is available.
	 *
	 * @param effect the name of the effect
	 * @return the availability of the effect or {@link TriState#UNKNOWN} if the effect has not been
	 * tested yet
	 */
	@NotNull
	@NonBlocking
	@CheckReturnValue
	TriState isEffectAvailable(@NotNull String effect);
}
