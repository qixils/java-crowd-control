package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

@SuppressWarnings("deprecation")
public class CrowdControlTests {
	private static final int PORT = 57575;

	@Test
	public void constructorTest() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password("password").build();
		server.shutdown();
		Assertions.assertEquals(PORT, server.getPort());
		Assertions.assertEquals(
				"b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",
				server.getPassword()
		);

		CrowdControl client = CrowdControl.client().port(PORT + 1).ip("localhost").build();
		client.shutdown("test");
		Assertions.assertEquals(PORT + 1, client.getPort());
		Assertions.assertNotNull(client.getIP());
		Assertions.assertEquals("localhost", client.getIP().getHostName());

		Thread.sleep(20);
	}

	@Test
	public void registerTests() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password("password").build();
		server.shutdown("test");
		Thread.sleep(10);

		for (int i = 1; i <= 14; i++) {
			Assertions.assertFalse(server.hasHandler(String.valueOf(i)));
		}

		server.registerHandler("1", $ -> {
		});
		server.registerHandler("2",
				(Function<Request, Response>) request -> request.buildResponse().type(Response.ResultType.SUCCESS).build()
		);
		//noinspection unused
		server.registerHandlers(new Object() {
			// true valid handlers

			@Subscribe("3")
			public Response test(Request request) {
				return request.buildResponse().type(Response.ResultType.SUCCESS).build();
			}

			@Subscribe("4")
			public Response.Builder test2(Request request) {
				return request.buildResponse().type(Response.ResultType.SUCCESS);
			}

			@Subscribe("5")
			public void test3(Request request) {
			}

			// "valid" handlers

			@Subscribe("6")
			public Response test4(Request request) {
				return null;
			}

			@Subscribe("7")
			public Response test5(Request request) throws IllegalAccessException {
				throw new IllegalAccessException();
			}

			@Subscribe("8")
			public void test6(Request request) throws IllegalAccessException {
				throw new IllegalAccessException();
			}

			@Subscribe("9")
			public void test7(Request request) {
			}

			@Subscribe("10")
			private void invalidTest0(Request request) {
			}

			// invalid handlers

			@Subscribe("11")
			public Object invalidTest1(Request request) {
				return new Object();
			}

			@Subscribe("12")
			public void invalidTest2(Object object) {
			}

			@Subscribe("13")
			public void invalidTest3(Object object1, Object object2) {
			}

			@Subscribe("14")
			public void invalidTest4() {
			}
		});

		for (int i = 1; i <= 9; i++) {
			Assertions.assertTrue(server.hasHandler(String.valueOf(i)), "Handler " + i + " not registered");
		}

		for (int i = 10; i <= 14; i++) {
			Assertions.assertFalse(server.hasHandler(String.valueOf(i)), "Handler " + i + " registered");
		}

		Assertions.assertThrows(IllegalArgumentException.class, () -> server.registerHandler("1", $ -> {
		}));
		Assertions.assertThrows(IllegalArgumentException.class, () -> server.registerHandler("1", (Function<Request, Response>) request -> request.buildResponse().type(Response.ResultType.SUCCESS).build()));
	}
}
