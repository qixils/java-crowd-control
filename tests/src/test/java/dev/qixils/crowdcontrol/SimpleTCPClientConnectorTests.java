package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@code SimpleTCPClientConnector} socket protocol.
 */
@SuppressWarnings("BusyWait")
public class SimpleTCPClientConnectorTests {
	private static final int PORT = 53736;
	private static final Object EFFECT_HANDLERS = new EffectHandlers();
	private static final String CORRECT_PASSWORD = "correct";
	private static final String INCORRECT_PASSWORD = "incorrect";

	@Test
	public void singleClientTest() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password(CORRECT_PASSWORD).build();
		server.registerHandlers(EFFECT_HANDLERS);

		SimulatedClient client = new SimulatedClient("localhost", PORT, CORRECT_PASSWORD);
		Assertions.assertDoesNotThrow(client::start);

		// wait for the server to start & client to connect
		int delay = 1;
		while (!client.isAcceptingRequests() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertTrue(client.isAcceptingRequests());

		// test request
		Response response = client.sendRequest(new Request.Builder().effect("success").viewer("test")).block();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(response.getResultType(), Response.ResultType.SUCCESS);

		// cleanup
		client.shutdown();
		server.shutdown("Test completed");

		Thread.sleep(40); // give server time to shut down
		Assertions.assertFalse(client.isRunning());
	}

	@Test
	public void multipleClientsTest() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password(CORRECT_PASSWORD).build();
		server.registerHandlers(EFFECT_HANDLERS);

		final int clients = 5;
		final List<SimulatedClient> clientList = new ArrayList<>(clients);
		for (int i = 0; i < clients; i++) {
			SimulatedClient client = new SimulatedClient("localhost", PORT, CORRECT_PASSWORD);
			Assertions.assertDoesNotThrow(client::start);
			clientList.add(client);
		}

		// wait for the server to start & client to connect
		int delay = 1;
		while (clientList.stream().anyMatch(client -> !client.isAcceptingRequests()) && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		// test request
		for (SimulatedClient client : clientList) {
			Assertions.assertTrue(client.isAcceptingRequests());
			Response response = client.sendRequest(new Request.Builder().effect("success").viewer("test")).block();
			Assertions.assertNotNull(response);
			Assertions.assertEquals(response.getResultType(), Response.ResultType.SUCCESS);
		}

		// cleanup
		clientList.forEach(SimulatedClient::shutdown);
		server.shutdown("Test completed");

		Thread.sleep(40); // give server time to shut down
	}

	@Test
	public void incorrectPasswordTest() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password(CORRECT_PASSWORD).build();
		server.registerHandlers(EFFECT_HANDLERS);

		SimulatedClient client = new SimulatedClient("localhost", PORT, INCORRECT_PASSWORD);
		Assertions.assertDoesNotThrow(client::start);

		// wait for the server to start & client to connect
		int delay = 1;
		while (!client.isShutdown() && delay <= 12) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertFalse(client.isAcceptingRequests());
		Assertions.assertTrue(client.isShutdown());

		// cleanup
		server.shutdown("Test completed");

		Thread.sleep(40); // give server time to shut down
	}
}
