package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
public class RequestTests {
	@Test
	public void constructorTest() {
		// negative ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(-1, Request.Type.START, "success", "qixils", null, null, null));
		// null type test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, null, "success", "qixils", null, null, null));
		// null effect test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, null, "qixils", null, null, null));
		// null viewer test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "success", null, null, null, null));
		// valid request test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.START, "success", "qixils", null, null, null));
		// valid builder test
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("success").viewer("qixils").build());
	}

	@Test
	public void getterTest() {
		Request request = new Request(1,
				Request.Type.START,
				"success",
				"qixils",
				"Hello",
				10,
				new Request.Target[]{new Request.Target(12345, "streamer", "https://i.qixils.dev/favicon.png")});
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.START, request.getType());
		Assertions.assertEquals("success", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(1, request.getTargets().length);
		Assertions.assertEquals(12345, request.getTargets()[0].getId());
		Assertions.assertEquals("streamer", request.getTargets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", request.getTargets()[0].getAvatar());
		Assertions.assertFalse(request.isGlobal());
	}

	@Test
	public void builderTest() {
		Request.Builder builder = new Request.Builder();

		// id test
		Assertions.assertEquals(-1, builder.id());
		builder = builder.id(1);
		Assertions.assertEquals(1, builder.id());

		// type test
		Assertions.assertEquals(Request.Type.START, builder.type());
		builder = builder.type(Request.Type.STOP);
		Assertions.assertEquals(Request.Type.STOP, builder.type());

		// effect test
		Assertions.assertNull(builder.effect());
		builder = builder.effect("success");
		Assertions.assertEquals("success", builder.effect());

		// viewer test
		Assertions.assertNull(builder.viewer());
		builder = builder.viewer("qixils");
		Assertions.assertEquals("qixils", builder.viewer());

		// message test
		Assertions.assertNull(builder.message());
		builder = builder.message("Hello");
		Assertions.assertEquals("Hello", builder.message());

		// cost test
		Assertions.assertNull(builder.cost());
		builder = builder.cost(10);
		Assertions.assertEquals(10, builder.cost());

		// targets test
		Assertions.assertNull(builder.targets());
		builder = builder.targets(new Request.Target[]{new Request.Target(12345, "streamer", "https://i.qixils.dev/favicon.png")});
		Assertions.assertEquals(1, builder.targets().length);
		Assertions.assertEquals(12345, builder.targets()[0].getId());
		Assertions.assertEquals("streamer", builder.targets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.targets()[0].getAvatar());

		// test cloning and building/toBuilder
		Request request = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.STOP, request.getType());
		Assertions.assertEquals("success", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(1, request.getTargets().length);
		Assertions.assertEquals(12345, request.getTargets()[0].getId());
		Assertions.assertEquals("streamer", request.getTargets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", request.getTargets()[0].getAvatar());
		Assertions.assertFalse(request.isGlobal());
	}

	@Test
	public void serializationTest() {
		Request request = new Request.Builder()
				.id(1)
				.type(Request.Type.START)
				.effect("success")
				.viewer("qixils")
				.message("Hello")
				.cost(10)
				.targets(new Request.Target[]{new Request.Target(12345, "streamer", "https://i.qixils.dev/favicon.png")})
				.clone()
				.build();
		String json = "{\"id\":1,\"type\":1,\"code\":\"success\",\"viewer\":\"qixils\",\"message\":\"Hello\",\"cost\":10,\"targets\":[{\"id\":12345,\"name\":\"streamer\",\"avatar\":\"https://i.qixils.dev/favicon.png\"}]}";
		Assertions.assertEquals(Request.fromJSON(request.toJSON()), Request.fromJSON(json));
	}
}
