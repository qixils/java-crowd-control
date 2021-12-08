package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
public class RequestTests {
	@Test
	public void constructorTests() {
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
	public void getterTests() {
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
	public void builderTests() {
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

		request = request.toBuilder()
				.id(2)
				.message(null)
				.cost(null)
				.targets(null)
				.clone()
				.build();
		Assertions.assertEquals(2, request.getId());
		Assertions.assertTrue(request.isGlobal());
		Assertions.assertEquals(0, request.getTargets().length);
		Assertions.assertNull(request.getMessage());
		Assertions.assertNull(request.getCost());
	}

	@Test
	public void builderGetterTests() {
		Request.Builder builder = new Request.Builder()
				.id(1)
				.type(Request.Type.START)
				.effect("success")
				.viewer("qixils")
				.message("Hello")
				.cost(10)
				.targets(new Request.Target[]{new Request.Target(12345, "streamer", "https://i.qixils.dev/favicon.png")})
				.clone();
		Assertions.assertEquals(1, builder.id());
		Assertions.assertEquals(Request.Type.START, builder.type());
		Assertions.assertEquals("success", builder.effect());
		Assertions.assertEquals("qixils", builder.viewer());
		Assertions.assertEquals("Hello", builder.message());
		Assertions.assertEquals(10, builder.cost());
		Assertions.assertEquals(1, builder.targets().length);
		Assertions.assertEquals(12345, builder.targets()[0].getId());
		Assertions.assertEquals("streamer", builder.targets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.targets()[0].getAvatar());

		builder.id(2);
		builder.message(null);
		builder.cost(null);
		builder.targets(null);
		Assertions.assertEquals(2, builder.id());
		Assertions.assertNull(builder.targets());
		Assertions.assertNull(builder.message());
		Assertions.assertNull(builder.cost());
	}
}
