package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.SimulatedServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for the {@code SimpleTCPConnector} socket protocol.
 */
@SuppressWarnings("BusyWait")
public final class SimpleTCPConnectorTests {
	private static final Object EFFECT_HANDLERS = new EffectHandlers();

	@Test
	public void singleClientTest() throws InterruptedException {
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

		// basically asserting that only one response is received
		Request.Builder request = new Request.Builder().effect("success").viewer("test");
		Flux<Response> flux = server.sendRequest(request).blockFirst();
		Assertions.assertNotNull(flux);
		CompletableFuture<Response> firstFuture = new CompletableFuture<>();
		CompletableFuture<Void> validationFuture = new CompletableFuture<>();
		flux.subscribe(
				response -> {
					if (firstFuture.isDone())
						validationFuture.completeExceptionally(new AssertionError("More than one response received"));
					else
						firstFuture.complete(response);
				},
				e -> {
					firstFuture.complete(null);
					validationFuture.completeExceptionally(e);
				},
				() -> {
					firstFuture.complete(null);
					validationFuture.complete(null);
				}
		);
		Assertions.assertDoesNotThrow(validationFuture::join);

		Response response = Assertions.assertDoesNotThrow(firstFuture::join);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());

		// cleanup
		client.shutdown("Test completed");
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}

	@Test
	public void multipleClientsTest() throws InterruptedException {
		SimulatedServer server = new SimulatedServer(0);
		Assertions.assertDoesNotThrow(server::start);

		final int clients = 5;

		List<CrowdControl> clientsList = new ArrayList<>(clients);
		for (int i = 0; i < clients; i++) {
			CrowdControl client = CrowdControl.client().ip("localhost").port(server.getPort()).build();
			client.registerHandlers(EFFECT_HANDLERS);
			clientsList.add(client);
		}

		// give clients time to connect
		int delay = 1;
		while (!server.isAcceptingRequests() && server.getConnectedClients() < clients && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// get all responses
		Request.Builder request = new Request.Builder().effect("success").viewer("test");
		Thread.sleep(40); // more delay for good measure
		List<Response> responses = server.sendRequest(request).mapNotNull(Flux::blockFirst).collectList().block();

		Assertions.assertNotNull(responses);
		Assertions.assertEquals(clients, responses.size());
		for (Response response : responses) {
			Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		}

		// cleanup
		clientsList.forEach(client -> client.shutdown("Test completed"));
		Thread.sleep(10);
		server.shutdown();

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(server.isRunning());
	}
}
