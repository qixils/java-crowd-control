package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.EffectUnavailableException;
import dev.qixils.crowdcontrol.socket.Request.Builder;
import dev.qixils.crowdcontrol.socket.Response;
import dev.qixils.crowdcontrol.socket.Response.ResultType;
import dev.qixils.crowdcontrol.socket.SimulatedServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Locale;

@SuppressWarnings("BusyWait")
public class EffectResponseTests {
	private static final Object EFFECT_HANDLERS = new EffectHandlers();

	private void basicTest(Response.ResultType resultType) throws InterruptedException {
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
		final String effectName = resultType.name().toLowerCase(Locale.ENGLISH);
		Builder builder1 = new Builder()
				.effect(effectName)
				.viewer("test");
		Builder builder2 = builder1.clone();
		Flux<Response> responseFlux = server.sendRequest(builder1).blockFirst();
		Assertions.assertNotNull(responseFlux);
		Response response = responseFlux.blockFirst();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(resultType, response.getResultType());

		if (resultType == ResultType.UNAVAILABLE) {
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

	@Test
	public void successResponse() throws InterruptedException {
		basicTest(ResultType.SUCCESS);
	}

	@Test
	public void failureResponse() throws InterruptedException {
		basicTest(ResultType.FAILURE);
	}

	@Test
	public void unavailableResponse() throws InterruptedException {
		basicTest(ResultType.UNAVAILABLE);
	}
}
