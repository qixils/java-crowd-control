package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class ResponseTests {
	@Test
	public void constructorTest() {
		Request request = new Request.Builder().effect("test").viewer("sdk").id(1).build();

		// Constructor 1

		// negative ID throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				-1,
				null,
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		));
		// null packet type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				null,
				"Effect applied successfully"
		));
		// effect type packet throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.EFFECT_RESULT,
				"Effect applied successfully"
		));
		// null message throws IllegalArgumentException when PacketType#isMessageRequired() is true
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.DISCONNECT,
				null
		));
		// null message doesn't throw when PacketType#isMessageRequired() is false
		Assertions.assertDoesNotThrow(() -> new Response(
				1,
				null,
				Response.PacketType.LOGIN,
				null
		));

		// Constructor 2

		// negative ID throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				-1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				1000
		));
		// negative timeRemaining throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				-1
		));
		// null result type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				null,
				"Effect applied successfully",
				1000
		));

		// Constructor 3

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				null,
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		));
		// null packet type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				null,
				"Effect applied successfully"
		));
		// effect type packet throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				Response.PacketType.EFFECT_RESULT,
				"Effect applied successfully"
		));
		// null message throws IllegalArgumentException when PacketType#isMessageRequired() is true
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				Response.PacketType.DISCONNECT,
				null
		));
		// null message doesn't throw when PacketType#isMessageRequired() is false
		Assertions.assertDoesNotThrow(() -> new Response(
				request,
				Response.PacketType.LOGIN,
				null
		));

		// Constructor 4

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				1000
		));
		// null result type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				null,
				"Effect applied successfully",
				1000
		));
		// negative timeRemaining throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				-1
		));
	}

	@Test
	public void getterTest() {
		// Constructor 1
		Response response = new Response(
				1,
				null,
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		);
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.LOGIN, response.getPacketType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());

		// Constructor 2
		response = new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				1000
		);
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(1000, response.getTimeRemaining());

		// Constructor 3
		response = new Response(
				new Request.Builder().id(1).type(Request.Type.KEEP_ALIVE).build(),
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		);
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.LOGIN, response.getPacketType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());

		// Constructor 4
		response = new Response(
				new Request.Builder().id(1).type(Request.Type.KEEP_ALIVE).build(),
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				1000
		);
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(1000, response.getTimeRemaining());
	}

	@Test
	public void builderTest() {
		// constructor 1 test
		Response.Builder builder = new Response.Builder(1, null).clone();
		Assertions.assertEquals(1, builder.id());
		Assertions.assertNull(builder.originatingSocket());

		// constructor 2 test
		builder = new Response.Builder(new Request.Builder().id(2).type(Request.Type.KEEP_ALIVE).build());
		Assertions.assertEquals(2, builder.id());
		Assertions.assertNull(builder.originatingSocket());

		// packet type test
		Assertions.assertNull(builder.packetType());
		builder = builder.packetType(Response.PacketType.EFFECT_RESULT);
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, builder.packetType());

		// message test
		Assertions.assertNull(builder.message());
		builder = builder.message("Effect applied successfully");
		Assertions.assertEquals("Effect applied successfully", builder.message());

		// time remaining test
		Assertions.assertEquals(0, builder.timeRemaining());
		builder = builder.timeRemaining(1000);
		Assertions.assertEquals(1000, builder.timeRemaining());
		builder = builder.timeRemaining(2, TimeUnit.SECONDS);
		Assertions.assertEquals(2000, builder.timeRemaining());
		builder = builder.timeRemaining(LocalDateTime.now().plusSeconds(3));
		Assertions.assertTrue(builder.timeRemaining() > 2000); // LocalDateTime-based offsets are inherently not exact
		builder = builder.timeRemaining(Duration.ofSeconds(4));
		Assertions.assertEquals(4000, builder.timeRemaining());

		// result type test
		Assertions.assertNull(builder.type());
		builder = builder.type(Response.ResultType.SUCCESS);
		Assertions.assertEquals(Response.ResultType.SUCCESS, builder.type());

		// build test (clone + toBuilder)
		Response response = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(2, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(4000, response.getTimeRemaining());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());

		// misc
		response = response.toBuilder().message(null).build();
		Assertions.assertEquals("SUCCESS", response.getMessage());

		response = response.toBuilder().packetType(null).build();
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
	}

	@Test
	public void serializationTest() {
		Response effectResponse = new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				1000
		);
		String json = "{\"id\":1,\"type\":0,\"message\":\"Effect applied successfully\",\"timeRemaining\":1000,\"status\":0}";
		Assertions.assertEquals(Response.fromJSON(effectResponse.toJSON()), Response.fromJSON(json));

		Response loginResponse = new Response(
				2,
				null,
				Response.PacketType.LOGIN_SUCCESS,
				"Login successful"
		);
		json = "{\"id\":2,\"type\":241,\"message\":\"Login successful\"}";
		Assertions.assertEquals(Response.fromJSON(loginResponse.toJSON()), Response.fromJSON(json));
	}
}
