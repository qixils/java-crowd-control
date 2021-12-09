package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class CrowdControlTests {
	private static final int PORT = 57575;

	@Test
	public void constructorTest() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password("password").build();
		server.shutdown("test");
		Assertions.assertEquals(PORT, server.getPort());
		Assertions.assertEquals(
				"b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",
				server.getPassword()
		);

		CrowdControl client = CrowdControl.client().port(PORT + 1).ip("localhost").build();
		client.shutdown("test");
		Assertions.assertEquals(PORT + 1, client.getPort());
		Assertions.assertEquals("localhost", client.getIP());

		Thread.sleep(20);
	}

	@Test
	public void registerTests() throws InterruptedException {
		CrowdControl server = CrowdControl.server().port(PORT).password("password").build();
		server.shutdown("test");
		Thread.sleep(10);

		for (int i = 1; i <= 9; i++) {
			Assertions.assertFalse(server.hasHandler(String.valueOf(i)));
		}

		server.registerHandler("1", $ -> {
		});
		server.registerHandler("2",
				(Function<Request, Response>) request -> new Response(request, Response.ResultType.SUCCESS, null, 0)
		);
		server.registerHandlers(new Object() {
			@Subscribe(effect = "3")
			public Response test(Request request) {
				return new Response(request, Response.ResultType.SUCCESS, null, 0);
			}

			@Subscribe(effect = "4")
			public Response.Builder test2(Request request) {
				return new Response(request, Response.ResultType.SUCCESS, null, 0).toBuilder();
			}

			@Subscribe(effect = "5")
			public void test3(Request request) {
			}

			@Subscribe(effect = "6")
			public Object invalidTest1(Request request) {
				return new Object();
			}

			@Subscribe(effect = "7")
			public void invalidTest2(Object object) {
			}

			@Subscribe(effect = "8")
			public void invalidTest3(Object object1, Object object2) {
			}

			@Subscribe(effect = "9")
			public void invalidTest4() {
			}
		});

		for (int i = 1; i <= 5; i++) {
			Assertions.assertTrue(server.hasHandler(String.valueOf(i)));
		}

		for (int i = 6; i <= 9; i++) {
			Assertions.assertFalse(server.hasHandler(String.valueOf(i)));
		}
	}
}
