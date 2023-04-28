package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.EffectUnavailableException;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.SimulatedClient;
import dev.qixils.crowdcontrol.socket.SimulatedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@SuppressWarnings("BusyWait")
public final class EffectResponseTests {
	private static final int PORT = 53737;
	private static final @NotNull String PASSWORD = "password";
	private static final @NotNull ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	private static final @NotNull Object EFFECT_HANDLERS = new EffectHandlers();

	private void basicTest(@NotNull String effectName,
						   Response.@NotNull ResultType resultType,
						   @Nullable Consumer<CrowdControl> clientModifier) throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		client.registerHandlers(EFFECT_HANDLERS);
		if (clientModifier != null)
			clientModifier.accept(client);

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// send effect
		Request.Builder builder = new Request.Builder()
				.effect(effectName)
				.viewer("test");
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(resultType, response.getResultType());

		Thread.sleep(30); // must wait for map to update??

		if (resultType == Response.ResultType.UNAVAILABLE) {
			Assertions.assertEquals(TriState.FALSE, server.isEffectAvailable(effectName));
			//noinspection ReactiveStreamsUnusedPublisher
			Assertions.assertThrows(EffectUnavailableException.class, () -> server.sendRequest(builder));
		} else {
			Assertions.assertEquals(TriState.TRUE, server.isEffectAvailable(effectName));
			Assertions.assertDoesNotThrow(() -> server.sendRequest(builder));
		}

		// cleanup
		client.shutdown(builder.build(), "Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	private void basicTest(Response.@NotNull ResultType resultType) throws InterruptedException {
		basicTest(resultType.name().toLowerCase(Locale.ENGLISH), resultType, null);
	}

	@Test
	public void successResponse() throws InterruptedException {
		basicTest(Response.ResultType.SUCCESS);
	}

	@Test
	public void failureResponse() throws InterruptedException {
		basicTest(Response.ResultType.FAILURE);
	}

	@Test
	public void unavailableResponse() throws InterruptedException {
		basicTest(Response.ResultType.UNAVAILABLE);
	}

	@Test
	public void globalCheckResponse() throws InterruptedException {
		basicTest("success", Response.ResultType.FAILURE,
				client -> client.registerCheck(() -> CheckResult.DISALLOW));
	}

	@Test
	public void variableCheckResponse() throws InterruptedException {
		Consumer<CrowdControl> modifier = client -> client.registerCheck(request ->
				Objects.equals(request.getEffect(), "check")
						? CheckResult.DISALLOW
						: CheckResult.ALLOW);
		basicTest("check", Response.ResultType.FAILURE, modifier);
		basicTest("success", Response.ResultType.SUCCESS, modifier);
	}

	@Test
	public void timeoutTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		client.registerHandlers(EFFECT_HANDLERS);

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// send effect
		Request.Builder builder = new Request.Builder()
				.effect("nothing")
				.viewer("test");
		Flux<Response> responseFlux = server.sendRequest(builder, Duration.ofMillis(50)).blockFirst();
		Assertions.assertNotNull(responseFlux);

		// janky way to check that the request timed out
		CompletableFuture<Throwable> future = new CompletableFuture<>();
		responseFlux.subscribe(null, future::complete, () -> future.complete(null));
		Throwable throwable = Assertions.assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
		Assertions.assertInstanceOf(TimeoutException.class, throwable);

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void noTimeoutTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		client.registerHandlers(EFFECT_HANDLERS);

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// send effect
		Request.Builder builder = new Request.Builder()
				.effect("nothing")
				.viewer("test");
		Flux<Response> responseFlux = server.sendRequest(builder, null).blockFirst();
		Assertions.assertNotNull(responseFlux);

		// janky way to check that the request... doesn't immediately time out?
		Assertions.assertThrows(IllegalStateException.class, () -> responseFlux.blockFirst(Duration.ofMillis(40)));

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test // test TimedEffect class + RETRY type
	public void timedEffectTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> firstFuture = new CompletableFuture<>();
		CompletableFuture<TimedEffect> secondFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers(timedEffect -> {
			if (!firstFuture.isDone())
				firstFuture.complete(null);
			else
				secondFuture.complete(timedEffect);
			return new Response.Builder();
		}));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// send first effect
		// note: the timed effect duration, as defined in EffectHandlers, is 0.2 seconds
		Request.Target target = new Request.Target.Builder().id("1").name("qixils").avatar("https://i.qixils.dev/favicon.png").build();
		Request.Builder builder = new Request.Builder()
				.effect("timed")
				.viewer("test")
				.targets(target);
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals(Duration.ofMillis(200), response.getTimeRemaining());

		// second request should initially "fail" (return RETRY)...
		Flux<Response> newFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(newFlux);
		response = newFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.RETRY, response.getResultType());
		Assertions.assertNull(response.getTimeRemaining());

		// (because the first effect is still running, which we should ensure is eventually going to finish)
		response = responseFlux.blockLast(Duration.ofSeconds(2));
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FINISHED, response.getResultType());
		Assertions.assertNull(response.getTimeRemaining());

		// ...but should eventually succeed, so let's set up some listeners
		CompletableFuture<Response> successDetector = new CompletableFuture<>();
		CompletableFuture<Response> pauseDetector = new CompletableFuture<>();
		CompletableFuture<Response> resumeDetector = new CompletableFuture<>();
		CompletableFuture<Response> finishDetector = new CompletableFuture<>();
		newFlux.subscribe(lResponse -> {
			if (lResponse.getResultType() == Response.ResultType.SUCCESS)
				successDetector.complete(lResponse);
			else if (lResponse.getResultType() == Response.ResultType.PAUSED)
				pauseDetector.complete(lResponse);
			else if (lResponse.getResultType() == Response.ResultType.RESUMED)
				resumeDetector.complete(lResponse);
			else if (lResponse.getResultType() == Response.ResultType.FINISHED)
				finishDetector.complete(lResponse);
		}, successDetector::completeExceptionally, () -> successDetector.complete(null));

		// and wait for the second effect to start
		response = successDetector.join();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals(Duration.ofMillis(200), response.getTimeRemaining());
		Thread.sleep(30); // wait for future to complete
		Assertions.assertTrue(secondFuture.isDone());

		// let's try pausing the effect
		TimedEffect effect = secondFuture.join();
		Assertions.assertNotNull(effect);
		Assertions.assertTrue(effect.hasStarted());
		Assertions.assertFalse(effect.isPaused());
		Assertions.assertFalse(effect.getCurrentDuration().isNegative() || effect.getCurrentDuration().isZero());
		Assertions.assertFalse(effect.isComplete());
		effect.pause();
		Assertions.assertTrue(effect.isPaused());
		response = pauseDetector.join();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.PAUSED, response.getResultType());
		Duration timeRemaining = response.getTimeRemaining();
		Assertions.assertFalse(timeRemaining == null || timeRemaining.isNegative());

		// while it's paused, let's test some miscellaneous methods
		Assertions.assertEquals(Duration.ofMillis(200), effect.getOriginalDuration());
		Assertions.assertThrows(IllegalStateException.class, effect::pause);
		Assertions.assertThrows(IllegalStateException.class, effect::queue);
		Request request = builder.build();
		Assertions.assertTrue(TimedEffect.isActive(request));
		Assertions.assertTrue(TimedEffect.isActive(request.getEffect(), target));
		Assertions.assertFalse(TimedEffect.isActive(request.getEffect()));
		Assertions.assertFalse(TimedEffect.isActive("blah", target));
		Assertions.assertFalse(TimedEffect.isActive("blah", request));

		// and now let's resume the effect
		effect.resume();
		Assertions.assertFalse(effect.isPaused());
		Assertions.assertThrows(IllegalStateException.class, effect::resume);
		response = resumeDetector.join();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.RESUMED, response.getResultType());
		Assertions.assertEquals(timeRemaining, response.getTimeRemaining());

		// finally, let's test the manual completion method
		Assertions.assertTrue(effect.complete());
		response = Assertions.assertDoesNotThrow(() -> finishDetector.get(30, TimeUnit.MILLISECONDS));
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FINISHED, response.getResultType());
		Assertions.assertNull(response.getTimeRemaining());

		// and validate some more methods
		Thread.sleep(10); // give the effect a chance to (get marked as) finish(ed)
		Assertions.assertFalse(TimedEffect.isActive(request));
		Assertions.assertThrows(IllegalStateException.class, effect::pause);
		Assertions.assertThrows(IllegalStateException.class, effect::resume);
		Assertions.assertFalse(effect.complete());
		Assertions.assertTrue(effect.isComplete());
		Assertions.assertEquals(Duration.ZERO, effect.getCurrentDuration());

		// truly finally, test the #isActive method in regard to global effects & custom names
		Request noTargetsRequest = effect.getRequest().toBuilder().targets((Request.Target[]) null).build();
		TimedEffect.Builder effectBuilder = new TimedEffect.Builder() // for creating identical TimedEffect instances
				.request(noTargetsRequest)
				.effectGroup("random_name")
				.duration(100)
				.startCallback($ -> noTargetsRequest.buildResponse());
		TimedEffect globalEffect = effectBuilder.build();
		globalEffect.queue();
		Assertions.assertTrue(globalEffect.hasStarted());
		Assertions.assertTrue(TimedEffect.isActive("random_name"));
		Assertions.assertTrue(TimedEffect.isActive("random_name", target));
		Assertions.assertTrue(TimedEffect.isActive("random_name", noTargetsRequest));
		Assertions.assertFalse(TimedEffect.isActive(noTargetsRequest));
		TimedEffect cannotStart = effectBuilder.build();
		cannotStart.queue();
		Assertions.assertFalse(cannotStart.hasStarted());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void failingTimedEffectTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers($ -> {
			completionFuture.complete(null);
			return new Response.Builder();
		}));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		Request.Builder builder = new Request.Builder()
				.effect("timedEffectError")
				.viewer("test");
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FAILURE, response.getResultType());
		Assertions.assertNull(response.getTimeRemaining());
		Thread.sleep(50); // wait for completion callback (not that it should execute)
		Assertions.assertFalse(completionFuture.isDone());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void customTimedEffectTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers($ -> {
			completionFuture.complete(null);
			return new Response.Builder();
		}));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		Request.Builder builder = new Request.Builder()
				.effect("timedEffectRetry")
				.viewer("test");
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.RETRY, response.getResultType());
		Assertions.assertNull(response.getTimeRemaining());
		Thread.sleep(50); // wait for completion callback (not that it should execute)
		Assertions.assertFalse(completionFuture.isDone());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void abruptCompletionTest1() throws InterruptedException, TimeoutException, ExecutionException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers(
				effect -> {
					EXECUTOR.schedule(() -> effect.complete(), 10, TimeUnit.MILLISECONDS);
					return new Response.Builder();
				},
				$ -> completionFuture.complete(null)
		));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		Request.Builder builder = new Request.Builder()
				.effect("timedEffectCompletionCallback")
				.viewer("test")
				.targets(new Request.Target.Builder().id("1").name("qixils").avatar("google.com").build());
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		// test first response
		CompletableFuture<Response> firstResponse = new CompletableFuture<>();
		CompletableFuture<Response> secondResponse = new CompletableFuture<>();
		responseFlux.subscribe(response -> {
			if (!firstResponse.complete(response))
				secondResponse.complete(response);
		});
		Response response = firstResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		// test second response
		response = secondResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FINISHED, response.getResultType());
		Thread.sleep(100); // wait for completion callback
		Assertions.assertTrue(completionFuture.isDone());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void abruptCompletionTest2() throws InterruptedException, TimeoutException, ExecutionException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers(
				effect -> {
					EXECUTOR.schedule(() -> effect.complete(true), 10, TimeUnit.MILLISECONDS);
					return new Response.Builder();
				},
				$ -> completionFuture.complete(null)
		));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		Request.Builder builder = new Request.Builder()
				.effect("timedEffectCompletionCallback")
				.viewer("test")
				.targets(new Request.Target.Builder().id("1").name("qixils").avatar("google.com").build());
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		// test first response
		CompletableFuture<Response> firstResponse = new CompletableFuture<>();
		CompletableFuture<Response> secondResponse = new CompletableFuture<>();
		responseFlux.subscribe(response -> {
			if (!firstResponse.complete(response))
				secondResponse.complete(response);
		});
		Response response = firstResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		// test second response
		response = secondResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FINISHED, response.getResultType());
		Thread.sleep(100); // wait for completion callback
		Assertions.assertTrue(completionFuture.isDone());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void abruptCompletionTest3() throws InterruptedException, TimeoutException, ExecutionException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();
		client.registerHandlers(new EffectHandlers(
				effect -> {
					EXECUTOR.schedule(() -> effect.complete(false), 10, TimeUnit.MILLISECONDS);
					return new Response.Builder();
				},
				$ -> completionFuture.complete(null)
		));

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		Request.Builder builder = new Request.Builder()
				.effect("timedEffectCompletionCallback")
				.viewer("test")
				.targets(new Request.Target.Builder().id("1").name("qixils").avatar("google.com").build());
		Flux<Response> responseFlux = server.sendRequest(builder).blockFirst();
		Assertions.assertNotNull(responseFlux);
		// test first response
		CompletableFuture<Response> firstResponse = new CompletableFuture<>();
		CompletableFuture<Response> secondResponse = new CompletableFuture<>();
		responseFlux.subscribe(response -> {
			if (!firstResponse.complete(response))
				secondResponse.complete(response);
		});
		Response response = firstResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		// test second response
		response = secondResponse.get(1, TimeUnit.SECONDS);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.FINISHED, response.getResultType());
		Thread.sleep(100); // wait for completion callback
		Assertions.assertFalse(completionFuture.isDone());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void successfulClientResponseBuilder() throws InterruptedException {
		// init server & client
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
		client.registerHandlers(EFFECT_HANDLERS);

		// give client time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// test response builder
		Response response = client.buildResponse().id(1).type(Response.ResultType.SUCCESS).build();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals(1, response.getId());
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertNull(response.getMessage());
		Assertions.assertTrue(response.isOriginKnown());
		Assertions.assertTrue(response::send);

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void unsuccessfulClientResponseBuilder() throws InterruptedException {
		// TODO (not sure how to force an IOException)
	}

	@Test
	public void successfulServerResponseBuilder() throws InterruptedException {
		// init server & client
		CrowdControl server = CrowdControl.server().port(PORT).password(PASSWORD).build();
		server.registerHandlers(EFFECT_HANDLERS);

		Thread.sleep(20); // give server time to start

		SimulatedClient client = new SimulatedClient("localhost", PORT, PASSWORD);
		Assertions.assertDoesNotThrow(client::start);

		// wait for the server to start & client to connect
		int delay = 1;
		while (!client.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(client.isAcceptingRequests());

		// test response builder
		Response response = server.buildResponse().id(1).type(Response.ResultType.SUCCESS).build();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals(1, response.getId());
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertNull(response.getMessage());
		Assertions.assertTrue(response.isOriginKnown());
		Assertions.assertTrue(response::send);

		// cleanup
		server.shutdown("Test completed");
		Thread.sleep(10);
		client.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(client.isRunning());
	}

	@Test
	public void unsuccessfulServerResponseBuilder() throws InterruptedException {
		// TODO (not sure how to force an IOException)
	}
}
