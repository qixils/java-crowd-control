package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@SuppressWarnings("ConstantConditions")
public class RequestTests {
	@SuppressWarnings("deprecation") // old constructors still need to be tested! :)
	@Test
	public void constructorTest() {
		// old constructor tests //

		// negative ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(-1, Request.Type.START, "summon", "qixils", null, null, null));
		// null type test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, null, "summon", "qixils", null, null, null));
		// null effect test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, null, "qixils", null, null, null));
		// null viewer test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", null, null, null, null));
		// negative cost test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, -1, null));
		// null target test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, null,
						new Request.Target[]{null}));
		// valid request test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, null, null));
		// also valid request test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.START, "summon", "qixils", "Purchased effect", 10,
						new Request.Target[]{new Request.Target("12345", "qixils", "https://i.qixils.dev/favicon.png")}));

		// new constructor tests //

		// negative ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(-1, Request.Type.START, "summon", "qixils", null, null, null, null, null));
		// null type test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, null, "summon", "qixils", null, null, null, null, null));
		// null effect test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, null, "qixils", null, null, null, null, null));
		// null viewer test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", null, null, null, null, null, null));
		// negative cost test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, -1, null, null, null));
		// negative duration test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, null, Duration.ofSeconds(-1), null, null));
		// null target test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, null, null,
						new Request.Target[]{null}, null));
		// valid request test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.START, "summon", "qixils", null, null, null, null, null));
		// also valid request test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.START, "summon", "qixils", "Purchased effect", 10, Duration.ofSeconds(10),
						new Request.Target[]{new Request.Target("12345", "qixils", "https://i.qixils.dev/favicon.png")}, new Object[]{1}));
		// valid builder test
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").build());
		// also valid builder test
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils")
						.message("Purchased effect").cost(10).duration(Duration.ofSeconds(10))
						.targets(new Request.Target("12345", "qixils", "https://i.qixils.dev/favicon.png"))
						.parameters(1).build());

		// non-effect constructor tests //

		// negative ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(-1, Request.Type.KEEP_ALIVE, null));
		// null type test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, null, null));
		// null password test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request(1, Request.Type.LOGIN, null));
		// valid keep-alive test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.KEEP_ALIVE, null));
		// valid login test
		Assertions.assertDoesNotThrow(
				() -> new Request(1, Request.Type.LOGIN, "password"));

		// target constructor tests //

		// null ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Target(null, "streamer", "https://i.qixils.dev/favicon.png"));
		// null name test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Target("1", null, "https://i.qixils.dev/favicon.png"));
		// null avatar test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Target("1", "streamer", null));
		// valid target test
		Assertions.assertDoesNotThrow(
				() -> new Request.Target("1", "streamer", "https://i.qixils.dev/favicon.png"));
	}

	@Test
	public void getterTest() {
		Request request = new Request(1,
				Request.Type.START,
				"summon",
				"qixils",
				"Hello",
				10,
				Duration.ofSeconds(10),
				new Request.Target[]{new Request.Target("12345", "streamer", "https://i.qixils.dev/favicon.png")},
				new Object[]{5});
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.START, request.getType());
		Assertions.assertEquals("summon", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(Duration.ofSeconds(10), request.getDuration());
		Assertions.assertEquals(1, request.getTargets().length);
		Assertions.assertEquals("12345", request.getTargets()[0].getId());
		Assertions.assertEquals("streamer", request.getTargets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", request.getTargets()[0].getAvatar());
		Assertions.assertEquals(1, request.getParameters().length);
		Assertions.assertEquals(5, request.getParameters()[0]);
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
		builder = builder.effect("summon");
		Assertions.assertEquals("summon", builder.effect());

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

		// duration test
		Assertions.assertNull(builder.duration());
		builder = builder.duration(Duration.ofSeconds(10));
		Assertions.assertEquals(Duration.ofSeconds(10), builder.duration());

		// targets test
		Assertions.assertNull(builder.targets());
		builder = builder.targets(new Request.Target("12345", "streamer", "https://i.qixils.dev/favicon.png"));
		Assertions.assertEquals(1, builder.targets().length);
		Assertions.assertEquals("12345", builder.targets()[0].getId());
		Assertions.assertEquals("streamer", builder.targets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.targets()[0].getAvatar());

		// parameters test
		Assertions.assertNull(builder.parameters());
		builder = builder.parameters(5);
		Assertions.assertEquals(1, builder.parameters().length);
		Assertions.assertEquals(5, builder.parameters()[0]);

		// test cloning and building/toBuilder
		Request request = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.STOP, request.getType());
		Assertions.assertEquals("summon", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(Duration.ofSeconds(10), request.getDuration());
		Assertions.assertEquals(1, request.getTargets().length);
		Assertions.assertEquals("12345", request.getTargets()[0].getId());
		Assertions.assertEquals("streamer", request.getTargets()[0].getName());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", request.getTargets()[0].getAvatar());
		Assertions.assertEquals(1, request.getParameters().length);
		Assertions.assertEquals(5, request.getParameters()[0]);
		Assertions.assertFalse(request.isGlobal());
	}

	@Test
	public void serializationTest() {
		// instead of directly comparing JSON strings which could have key order differences,
		// we create an example object, serialize it, deserialize it, and then compare it to
		// another object created from a deserialization of the hopefully equivalent JSON string
		Request request = new Request.Builder()
				.id(1)
				.type(Request.Type.START)
				.effect("summon")
				.viewer("qixils")
				.message("Hello")
				.cost(10)
				.duration(Duration.ofSeconds(10))
				.targets(new Request.Target("12345", "streamer", "https://i.qixils.dev/favicon.png"))
				.parameters(5)
				.clone()
				.build();
		String json = "{\"id\":1,\"type\":1,\"code\":\"summon\",\"viewer\":\"qixils\",\"message\":\"Hello\",\"cost\":10,\"duration\":10000,\"targets\":[{\"id\":12345,\"name\":\"streamer\",\"avatar\":\"https://i.qixils.dev/favicon.png\"}],\"parameters\":[5]}";
		Assertions.assertEquals(Request.fromJSON(request.toJSON()), Request.fromJSON(json));
	}
}
