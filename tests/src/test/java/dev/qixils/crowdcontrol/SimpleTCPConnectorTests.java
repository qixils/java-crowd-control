package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.Response.ResultType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

public class SimpleTCPConnectorTests {
	private static final int PORT = 53735;

	@Test
	public void successfulRequestSingleClientTest() {
		SimulatedServer server = new SimulatedServer(PORT);
		Assertions.assertDoesNotThrow(server::start);

		CrowdControl client = CrowdControl.client().ip("localhost").port(PORT).build();
		client.registerHandlers(new EffectHandlers());

		// give client time to connect
		try {
			int delay = 1;
			while (!server.isAcceptingRequests() && delay <= 10) {
				Thread.sleep((long) Math.pow(2, delay++));
			}
		} catch (InterruptedException e) {
			Assertions.fail(e);
		}

		Assertions.assertTrue(server.isAcceptingRequests());

		// basically asserting that only one response is received
		Request.Builder request = new Request.Builder().effect("success").viewer("test");
		Flux<Response> flux = server.sendRequest(request);
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
		Assertions.assertEquals(response.getResultType(), ResultType.SUCCESS);
	}
}
