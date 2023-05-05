package dev.qixils.crowdcontrol.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@SuppressWarnings("ConstantConditions")
public class RequestTests {
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void constructorTest() {
		// new constructor tests //

		// negative ID test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(-1).type(Request.Type.START).effect("summon").viewer("qixils").build());
		// null type test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).effect("summon").viewer("qixils").build());
		// null effect test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).viewer("qixils").build());
		// null viewer test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").build());
		// negative cost test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").cost(-1).build());
		// negative duration test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").duration(Duration.ofSeconds(-1)).build());
		// null target test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").targets((Request.Target) null).build());
		// using value with non-remote function result test
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").value(true).build());
		// valid builder test
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils").build());
		// also valid builder test
		Request.Target target = new Request.Target.Builder().id("12345").name("qixils").avatar("https://i.qixils.dev/favicon.png").build();
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.START).effect("summon").viewer("qixils")
						.message("Purchased effect").cost(10).duration(Duration.ofSeconds(10))
						.targets(target).parameters(1d).quantity(3)
						.source(new Request.Source.Builder().target(target).build()).build());
		// remote function result test
		Assertions.assertDoesNotThrow(
				() -> new Request.Builder().id(1).type(Request.Type.REMOTE_FUNCTION_RESULT).value(true).build());

		// target builder tests //
		Assertions.assertDoesNotThrow(
				() -> new Request.Target.Builder().build());
		Assertions.assertDoesNotThrow(
				() -> new Request.Target.Builder().id("493").name("epic streamer 493").login("streamer").avatar("https://i.qixils.dev/favicon.png").clone().build());
	}

	@Test
	public void getterTest() {
//		Request request = new Request(1,
//				Request.Type.START,
//				"summon",
//				"qixils",
//				"Hello",
//				10,
//				Duration.ofSeconds(10),
//				new Request.Target[]{
//						new Request.Target.Builder().id("493").name("epic streamer 493").login("streamer").avatar("https://i.qixils.dev/favicon.png").clone().build().toBuilder().build(),
//						new Request.Target.Builder().clone().build().toBuilder().build()
//				},
//				new Object[]{5d});
		Request.Target source = new Request.Target.Builder().id("493").name("epic streamer 493").login("streamer").avatar("https://i.qixils.dev/favicon.png").service("TWITCH").clone().build();
		Request request = new Request.Builder()
				.id(1)
				.type(Request.Type.START)
				.effect("summon")
				.viewer("qixils")
				.message("Hello")
				.cost(10)
				.duration(Duration.ofSeconds(10))
				.targets(
						source,
						new Request.Target.Builder().clone().build()
				)
				.parameters(5d)
				.quantity(3)
				.source(new Request.Source.Builder().target(source).build())
				.build();
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.START, request.getType());
		Assertions.assertEquals("summon", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(Duration.ofSeconds(10), request.getDuration());
		Assertions.assertEquals(1, request.getParameters().length);
		Assertions.assertEquals(5d, request.getParameters()[0]);
		Assertions.assertFalse(request.isGlobal());
		Assertions.assertEquals(3, request.getTargets().length);
		Assertions.assertEquals(3, request.getQuantity());
		// target 1
		Assertions.assertEquals("493", request.getTargets()[0].getId());
		Assertions.assertEquals("epic streamer 493", request.getTargets()[0].getName());
		Assertions.assertEquals("streamer", request.getTargets()[0].getLogin());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", request.getTargets()[0].getAvatar());
		Assertions.assertEquals("TWITCH", request.getTargets()[0].getService());
		Assertions.assertEquals(source, request.getTargets()[0]);
		Assertions.assertEquals(source, request.getSource().target());
		// target 2
		Assertions.assertNull(request.getTargets()[1].getId());
		Assertions.assertNull(request.getTargets()[1].getName());
		Assertions.assertNull(request.getTargets()[1].getLogin());
		Assertions.assertNull(request.getTargets()[1].getAvatar());
		Assertions.assertNull(request.getTargets()[1].getService());
	}

	@Test
	public void builderTest() {
		Request.Builder builder = new Request.Builder();

		// id test
		Assertions.assertEquals(0, builder.id());
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

		// target builder test
		Request.Target.Builder targetBuilder = new Request.Target.Builder();
		Assertions.assertNull(targetBuilder.id());
		Assertions.assertEquals("493", targetBuilder.id("493").clone().build().toBuilder().id());
		Assertions.assertNull(targetBuilder.name());
		Assertions.assertEquals("epic streamer 493", targetBuilder.name("epic streamer 493").clone().build().toBuilder().name());
		Assertions.assertNull(targetBuilder.login());
		Assertions.assertEquals("streamer", targetBuilder.login("streamer").clone().build().toBuilder().login());
		Assertions.assertNull(targetBuilder.avatar());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", targetBuilder.avatar("https://i.qixils.dev/favicon.png").clone().build().toBuilder().avatar());
		Assertions.assertNull(targetBuilder.service());
		Assertions.assertEquals("TWITCH", targetBuilder.service("TWITCH").clone().build().toBuilder().service());

		// targets test
		Assertions.assertNull(builder.targets());
		builder = builder.targets(
				targetBuilder.build(),
				new Request.Target.Builder().clone().build().toBuilder().build()
		);
		Assertions.assertEquals(3, builder.targets().length);
		// target 1
		Assertions.assertEquals("493", builder.targets()[0].getId());
		Assertions.assertEquals("epic streamer 493", builder.targets()[0].getName());
		Assertions.assertEquals("streamer", builder.targets()[0].getLogin());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.targets()[0].getAvatar());
		Assertions.assertEquals("TWITCH", builder.targets()[0].getService());
		// target 2
		Assertions.assertNull(builder.targets()[1].getId());
		Assertions.assertNull(builder.targets()[1].getName());
		Assertions.assertNull(builder.targets()[1].getLogin());
		Assertions.assertNull(builder.targets()[1].getAvatar());
		Assertions.assertNull(builder.targets()[1].getService());

		// source builder test
		Request.Source.Builder sourceBuilder = new Request.Source.Builder();
		Assertions.assertNull(sourceBuilder.target());
		Assertions.assertEquals("493", sourceBuilder.target(targetBuilder.build()).clone().build().toBuilder().target().getId());
		Assertions.assertEquals("epic streamer 493", sourceBuilder.target().getName());
		Assertions.assertEquals("streamer", sourceBuilder.target().getLogin());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", sourceBuilder.target().getAvatar());
		Assertions.assertEquals("TWITCH", sourceBuilder.target().getService());
		Assertions.assertEquals(targetBuilder.build(), sourceBuilder.target());
		Assertions.assertNull(sourceBuilder.ip());
		Assertions.assertEquals("127.0.0.1", sourceBuilder.ip("127.0.0.1").clone().build().toBuilder().ip());

		// parameters test
		Assertions.assertNull(builder.parameters());
		builder = builder.parameters(5d);
		Assertions.assertEquals(1, builder.parameters().length);
		Assertions.assertEquals(5d, builder.parameters()[0]);

		// quantity test
		Assertions.assertNull(builder.quantity());
		Assertions.assertEquals(4, builder.quantity(4).quantity());

		// test cloning and building/toBuilder
		Request request = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(1, request.getId());
		Assertions.assertEquals(Request.Type.STOP, request.getType());
		Assertions.assertEquals("summon", request.getEffect());
		Assertions.assertEquals("qixils", request.getViewer());
		Assertions.assertEquals("Hello", request.getMessage());
		Assertions.assertEquals(10, request.getCost());
		Assertions.assertEquals(Duration.ofSeconds(10), request.getDuration());
		Assertions.assertEquals(1, request.getParameters().length);
		Assertions.assertEquals(5d, request.getParameters()[0]);
		Assertions.assertFalse(request.isGlobal());
		Assertions.assertEquals(3, builder.targets().length);
		Assertions.assertEquals(4, request.getQuantity());
		Assertions.assertEquals(1, request.toBuilder().quantity(null).build().getQuantityOrDefault());
		Assertions.assertNull(request.toBuilder().quantity(null).build().getQuantity());
		// target 1
		Assertions.assertEquals("493", builder.targets()[0].getId());
		Assertions.assertEquals("epic streamer 493", builder.targets()[0].getName());
		Assertions.assertEquals("streamer", builder.targets()[0].getLogin());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.targets()[0].getAvatar());
		Assertions.assertEquals("TWITCH", builder.targets()[0].getService());
		// target 2
		Assertions.assertNull(builder.targets()[1].getId());
		Assertions.assertNull(builder.targets()[1].getName());
		Assertions.assertNull(builder.targets()[1].getLogin());
		Assertions.assertNull(builder.targets()[1].getAvatar());
		Assertions.assertNull(builder.targets()[1].getService());
		// source
		Assertions.assertEquals("493", builder.source().target().getId());
		Assertions.assertEquals("epic streamer 493", builder.source().target().getName());
		Assertions.assertEquals("streamer", builder.source().target().getLogin());
		Assertions.assertEquals("https://i.qixils.dev/favicon.png", builder.source().target().getAvatar());
		Assertions.assertEquals("TWITCH", builder.source().target().getService());
		Assertions.assertEquals(targetBuilder.build(), builder.source().target());
		Assertions.assertEquals("127.0.0.1", builder.source().ip());
	}

	@Test
	public void serializationTest() {
		// instead of directly comparing JSON strings which could have key order differences,
		// we create an example object, serialize it, deserialize it, and then compare it to
		// another object created from a deserialization of the hopefully equivalent JSON string
		Request.Target target = new Request.Target.Builder().id("493").name("epic streamer 493").login("streamer").avatar("https://i.qixils.dev/favicon.png").service("TWITCH").clone().build().toBuilder().build();
		Request request = new Request.Builder()
				.id(1)
				.type(Request.Type.START)
				.effect("summon")
				.viewer("qixils")
				.message("Hello")
				.cost(10)
				.duration(Duration.ofSeconds(10))
				.targets(
						target,
						new Request.Target.Builder().clone().build().toBuilder().build())
				.parameters(5d) // json treats number params as doubles by default
				.quantity(3)
				.source(new Request.Source.Builder().target(target).ip("127.0.0.1").clone().build().toBuilder().build())
				.clone()
				.build();
		String json = "{\"id\":1,\"type\":1,\"code\":\"summon\",\"viewer\":\"qixils\",\"message\":\"Hello\",\"cost\":10,\"duration\":10000,\"targets\":[{\"id\":\"493\",\"name\":\"epic streamer 493\",\"login\":\"streamer\",\"avatar\":\"https://i.qixils.dev/favicon.png\",\"source\":\"TWITCH\"},{}],\"parameters\":[5],\"quantity\":3}";
		Assertions.assertEquals(request, Request.fromJSON(json));
		Assertions.assertEquals(Request.fromJSON(request.toJSON()), Request.fromJSON(json));
	}
}
