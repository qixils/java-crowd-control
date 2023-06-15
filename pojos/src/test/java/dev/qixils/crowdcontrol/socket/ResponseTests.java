package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class ResponseTests {
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

		response = new Response.Builder().packetType(Response.PacketType.REMOTE_FUNCTION).method("method").addArguments("arg1", 2, 3.0).addArguments(Arrays.asList("arg4", 5, 6.0)).clone().build().toBuilder().build();
		Assertions.assertEquals(Response.PacketType.REMOTE_FUNCTION, response.getPacketType());
		Assertions.assertEquals("method", response.getMethod());
		Assertions.assertArrayEquals(new Object[]{"arg1", 2, 3.0, "arg4", 5, 6.0}, response.getArguments());
	}

	@Test
	public void serializationTest() {
		Response effectResponse = new Response.Builder()
				.id(1)
				.type(Response.ResultType.SUCCESS)
				.message("Effect applied successfully")
				.timeRemaining(Duration.ofSeconds(1))
				.build();
		String json = "{\"id\":1,\"type\":0,\"message\":\"Effect applied successfully\",\"timeRemaining\":1000,\"status\":0}";
		Assertions.assertEquals(Response.fromJSON(effectResponse.toJSON()), Response.fromJSON(json));

		Response loginResponse = new Response.Builder()
				.packetType(Response.PacketType.LOGIN_SUCCESS)
				.message("Login successful")
				.build();
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
