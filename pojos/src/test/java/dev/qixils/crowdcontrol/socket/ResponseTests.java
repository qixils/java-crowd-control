package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class ResponseTests {
	@Test
	public void constructorTest() {
		Request request = new Request.Builder().effect("test").viewer("sdk").id(1).build();

		// Constructor 1

		// null packet type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				(Socket) null,
				null,
				"Server is disconnecting"
		));
		// effect result packet throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				(Socket) null,
				Response.PacketType.EFFECT_RESULT,
				"Effect applied successfully"
		));
		// null message throws IllegalArgumentException when PacketType#isMessageRequired() is true
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				(Socket) null,
				Response.PacketType.DISCONNECT,
				null
		));
		// null message doesn't throw when PacketType#isMessageRequired() is false
		Assertions.assertDoesNotThrow(() -> new Response(
				(Socket) null,
				Response.PacketType.LOGIN,
				null
		));
		// doesn't throw when all parameters are valid
		Assertions.assertDoesNotThrow(() -> new Response(
				(Socket) null,
				Response.PacketType.DISCONNECT,
				"Server is disconnecting"
		));

		// Constructor 2

		// negative ID throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				-1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				null
		));
		// non-positive timeRemaining throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ZERO
		));
		// null result type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				null,
				"Effect applied successfully",
				null
		));
		// doesn't throw when all parameters are valid
		Assertions.assertDoesNotThrow(() -> new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ofSeconds(10)
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				null
		));

		// Constructor 3

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				(Request) null,
				Response.PacketType.DISCONNECT,
				"Server is disconnecting"
		));
		// null packet type throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				request,
				null,
				"Server is disconnecting"
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
		// doesn't throw when all parameters are valid
		Assertions.assertDoesNotThrow(() -> new Response(
				request,
				Response.PacketType.DISCONNECT,
				"Server is disconnecting"
		));

		// Main constructor

		// negative ID throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				-1,
				null,
				Response.PacketType.EFFECT_RESULT,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				null,
				null,
				null,
				null
		));
		// non-zero ID throws IllegalArgumentException when PacketType is not EFFECT_RESULT
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.DISCONNECT,
				null,
				"Server is disconnecting",
				null,
				null,
				null,
				null
		));
		// negative timeRemaining throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.EFFECT_RESULT,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ofSeconds(-1),
				null,
				null,
				null
		));
		// zero timeRemaining throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.EFFECT_RESULT,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ZERO,
				null,
				null,
				null
		));
		// when packetType requires a result type and type is null, throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				1,
				null,
				Response.PacketType.EFFECT_RESULT,
				null,
				"Effect applied successfully",
				null,
				null,
				null,
				null
		));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.EFFECT_STATUS,
				null,
				null,
				null,
				null,
				null,
				null
		));
		// when packetType does not require a result type and type is not null, throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.DISCONNECT,
				Response.ResultType.SUCCESS,
				"Server is disconnecting",
				null,
				null,
				null,
				null
		));
		// when message is null and packetType requires a message, throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.DISCONNECT,
				null,
				null,
				null,
				null,
				null,
				null
		));
		// when effect is null and packetType is EFFECT_STATUS, throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.EFFECT_STATUS,
				Response.ResultType.VISIBLE,
				null,
				null,
				null,
				null,
				null
		));
		// when type is not a status and packetType is EFFECT_STATUS
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.EFFECT_STATUS,
				Response.ResultType.SUCCESS,
				null,
				null,
				null,
				null,
				null
		));
		// when type is a status and packetType is not EFFECT_STATUS
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.DISCONNECT,
				Response.ResultType.VISIBLE,
				null,
				null,
				null,
				null,
				null
		));
		// when packetType is REMOTE_FUNCTION and method is null, throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Response(
				0,
				null,
				Response.PacketType.REMOTE_FUNCTION,
				null,
				null,
				null,
				null,
				null,
				null
		));
		// valid parameters don't throw
		Assertions.assertDoesNotThrow(() -> new Response(
				0,
				null,
				Response.PacketType.DISCONNECT,
				null,
				"Server is disconnecting",
				null,
				null,
				null,
				null
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				1,
				null,
				null,
				Response.ResultType.SUCCESS,
				null,
				null,
				null,
				null,
				null
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				1,
				null,
				Response.PacketType.EFFECT_RESULT,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ofSeconds(1),
				"effect",
				null,
				null
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				0,
				null,
				Response.PacketType.EFFECT_STATUS,
				Response.ResultType.VISIBLE,
				null,
				null,
				"effect",
				null,
				null
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				0,
				null,
				Response.PacketType.REMOTE_FUNCTION,
				null,
				null,
				null,
				null,
				"method",
				null
		));
		Assertions.assertDoesNotThrow(() -> new Response(
				0,
				null,
				Response.PacketType.REMOTE_FUNCTION,
				null,
				null,
				null,
				null,
				"method",
				new Object[]{"arg1", 2, 3.0}
		));
	}

	@Test
	public void getterTest() {
		// Constructor 1
		Response response = new Response(
				(Socket) null,
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		);
		Assertions.assertEquals(0, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.LOGIN, response.getPacketType());
		Assertions.assertNull(response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertNull(response.getTimeRemaining());

		// Constructor 2
		response = new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ofSeconds(1)
		);
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(Duration.ofSeconds(1), response.getTimeRemaining());

		// Constructor 3
		response = new Response(
				new Request.Builder().type(Request.Type.KEEP_ALIVE).build(),
				Response.PacketType.LOGIN,
				"Effect applied successfully"
		);
		Assertions.assertEquals(0, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.LOGIN, response.getPacketType());
		Assertions.assertNull(response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertNull(response.getTimeRemaining());
	}

	@Test
	public void builderTest() {
		// constructor 1 test
		Response.Builder builder = new Request.Builder().id(3).effect("effect").viewer("sdk").type(Request.Type.START).build().buildResponse();
		Assertions.assertEquals(3, builder.id());
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
		Assertions.assertNull(builder.timeRemaining());
		builder = builder.timeRemaining(1000);
		Assertions.assertEquals(Duration.ofSeconds(1), builder.timeRemaining());
		builder = builder.timeRemaining(2, TimeUnit.SECONDS);
		Assertions.assertEquals(Duration.ofSeconds(2), builder.timeRemaining());
		builder = builder.timeRemaining(Instant.now().plusSeconds(3));
		Assertions.assertFalse(builder.timeRemaining().minusSeconds(2).isNegative());
		builder = builder.timeRemaining(Duration.ofSeconds(4));
		Assertions.assertEquals(Duration.ofSeconds(4), builder.timeRemaining());
		builder = builder.timeRemaining(5, ChronoUnit.SECONDS);
		Assertions.assertEquals(Duration.ofSeconds(5), builder.timeRemaining());

		// result type test
		Assertions.assertNull(builder.type());
		builder = builder.type(Response.ResultType.SUCCESS);
		Assertions.assertEquals(Response.ResultType.SUCCESS, builder.type());

		// build test (clone + toBuilder)
		Response response = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(3, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(Duration.ofSeconds(5), response.getTimeRemaining());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());

		// misc
		response = response.toBuilder().message(null).clone().build().toBuilder().build();
		Assertions.assertNull(response.getMessage());

		response = response.toBuilder().packetType(null).clone().build().toBuilder().build();
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());

		response = new Response.Builder().packetType(Response.PacketType.REMOTE_FUNCTION).addArguments("arg1", 2, 3.0).addArguments(Arrays.asList("arg4", 5, 6.0)).clone().build().toBuilder().build();
		Assertions.assertEquals(Response.PacketType.REMOTE_FUNCTION, response.getPacketType());
		Assertions.assertArrayEquals(new Object[]{"arg1", 2, 3.0, "arg4", 5, 6.0}, response.getArguments());
	}

	@Test
	public void serializationTest() {
		Response effectResponse = new Response(
				1,
				null,
				Response.ResultType.SUCCESS,
				"Effect applied successfully",
				Duration.ofSeconds(1)
		);
		String json = "{\"id\":1,\"type\":0,\"message\":\"Effect applied successfully\",\"timeRemaining\":1000,\"status\":0}";
		Assertions.assertEquals(Response.fromJSON(effectResponse.toJSON()), Response.fromJSON(json));

		Response loginResponse = new Response(
				(Socket) null,
				Response.PacketType.LOGIN_SUCCESS,
				"Login successful"
		);
		json = "{\"id\":0,\"type\":241,\"message\":\"Login successful\"}";
		Assertions.assertEquals(Response.fromJSON(loginResponse.toJSON()), Response.fromJSON(json));
	}

	@Test
	public void fromRequestBuilder() {
		Request request = new Request.Builder()
				.id(1)
				.effect("test")
				.viewer("qixils")
				.type(Request.Type.START)
				.build();

		// test available response
		Response response = request.buildResponse()
				.type(Response.ResultType.SUCCESS)
				.message("Effect applied successfully")
				.timeRemaining(1000)
				.build();
		Assertions.assertEquals(Response.PacketType.EFFECT_RESULT, response.getPacketType());
		Assertions.assertEquals(Response.ResultType.SUCCESS, response.getResultType());
		Assertions.assertEquals("Effect applied successfully", response.getMessage());
		Assertions.assertEquals(Duration.ofSeconds(1), response.getTimeRemaining());
		Assertions.assertEquals(1, response.getId());
		Assertions.assertFalse(response.isOriginKnown());
	}
}
