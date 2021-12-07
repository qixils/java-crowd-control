package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Response;
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
	 * throw a {@link java.util.concurrent.TimeoutException} if no response is received in a timely manner,
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(@NotNull Request request) {
		return sendRequest(request, true);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param builder the {@link Builder} to dispatch
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if no response is received in a timely manner,
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(Request.@NotNull Builder builder) {
		return sendRequest(builder, true);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param request the {@link Request} to dispatch
	 * @param timeout whether to throw a {@link java.util.concurrent.TimeoutException} if no
	 *                response is received in a timely manner
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if {@code timeout} is {@code true},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 */
	@NotNull
	@NonBlocking
	default Flux<@NotNull R> sendRequest(@NotNull Request request, boolean timeout) {
		return sendRequest(request.toBuilder(), timeout);
	}

	/**
	 * Dispatches a {@link Request} to the connected video game(s).
	 *
	 * @param builder the {@link Builder} to dispatch
	 * @param timeout whether to throw a {@link java.util.concurrent.TimeoutException} if no
	 *                response is received in a timely manner
	 * @return a {@link Publisher} that will either emit the {@link Response}(s),
	 * throw a {@link java.util.concurrent.TimeoutException} if {@code timeout} is {@code true},
	 * throw a {@link IOException} if an I/O error occurs trying to write the request,
	 * or throw a {@link IllegalStateException} if the simulated server or client is not
	 * {@link #isAcceptingRequests() accepting requests}
	 */
	@NotNull
	@NonBlocking
	Flux<@NotNull R> sendRequest(Request.@NotNull Builder builder, boolean timeout);
}
