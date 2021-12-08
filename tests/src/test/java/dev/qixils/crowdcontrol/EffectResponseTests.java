package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.EffectUnavailableException;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.SimulatedServer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@SuppressWarnings("BusyWait")
public class EffectResponseTests {
	private static final Object EFFECT_HANDLERS = new EffectHandlers();

	private void basicTest(String effectName,
						   Response.ResultType resultType,
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
		Request.Builder builder1 = new Request.Builder()
				.effect(effectName)
				.viewer("test");
		Request.Builder builder2 = builder1.clone();
		Flux<Response> responseFlux = server.sendRequest(builder1).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(resultType, response.getResultType());

		Thread.sleep(30); // must wait for map to update??

		if (resultType == Response.ResultType.UNAVAILABLE) {
			Assertions.assertEquals(TriState.FALSE, server.isEffectAvailable(effectName));
			//noinspection ReactiveStreamsUnusedPublisher
			Assertions.assertThrows(EffectUnavailableException.class, () -> server.sendRequest(builder2));
		} else {
			Assertions.assertEquals(TriState.TRUE, server.isEffectAvailable(effectName));
			Assertions.assertDoesNotThrow(() -> server.sendRequest(builder2));
		}

		// cleanup
		client.shutdown("Test completed");
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	private void basicTest(Response.ResultType resultType) throws InterruptedException {
		basicTest(resultType.name().toLowerCase(Locale.ENGLISH), resultType, client -> client.registerHandlers(EFFECT_HANDLERS));
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
				request.getEffect().equals("check")
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
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	// TODO: retry, others
}
