package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Miscellaneous tests for the TimedEffect class that can be run in isolation.
 */
@SuppressWarnings({"ConstantConditions", "BusyWait"})
public class TimedEffectTests {
	private static final @NotNull Request.Source SOURCE = new Request.Source.Builder().target(new Request.Target.Builder().id("12345").name("qixils").avatar("https://i.qixils.dev/favicon.png").build()).build();
	private static final @NotNull Request request = new Request.Builder()
			.id(1)
			.type(Request.Type.START)
			.effect("test")
			.viewer("qixils")
			.message("Hello World!")
			.cost(10)
			.duration(Duration.ofSeconds(15))
			.targets(SOURCE.target())
//			.parameters(5)
			.quantity(3)
			.source(SOURCE)
			.build();

	@Test
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void constructorTest() {
		TimedEffect.Builder builder = new TimedEffect.Builder()
				.request(request)
				.effectGroup("test")
				.duration(Duration.ofSeconds(10))
				.startCallback(effect -> null)
				.pauseCallback(effect -> {})
				.resumeCallback(effect -> {})
				.completionCallback(effect -> {})
				.blocks(false)
				.waits(false);

		// valid test
		Assertions.assertDoesNotThrow(builder::build);

		// valid null duration test
		Assertions.assertDoesNotThrow(() -> builder.clone().duration(null).build());

		// valid null effect group test
		Assertions.assertDoesNotThrow(() -> builder.clone().effectGroup(null).build());

		// valid null pause callback test
		Assertions.assertDoesNotThrow(() -> builder.clone().pauseCallback(null).build());

		// valid null resume callback test
		Assertions.assertDoesNotThrow(() -> builder.clone().resumeCallback(null).build());

		// valid null completion callback test
		Assertions.assertDoesNotThrow(() -> builder.clone().completionCallback(null).build());

		// invalid negative duration test
		Assertions.assertThrows(IllegalArgumentException.class, () -> builder.clone().duration(Duration.ofSeconds(-1)).build());

		// invalid null request test
		Assertions.assertThrows(IllegalArgumentException.class, () -> builder.clone().request(null).build());

		// invalid null start callback test
		Assertions.assertThrows(IllegalArgumentException.class, () -> builder.clone().startCallback(null).build());
	}

	@Test
	public void methodTests() {
		TimedEffect timedEffect = new TimedEffect.Builder()
				.request(request)
				.duration(1000)
				.startCallback($ -> new Response.Builder())
				.build();

		// getters
		Assertions.assertEquals(request, timedEffect.getRequest());
		Assertions.assertEquals(request.getEffect(), timedEffect.getEffectGroup());
		Assertions.assertEquals(Duration.ofSeconds(1), timedEffect.getCurrentDuration());
		Assertions.assertEquals(Duration.ofSeconds(1), timedEffect.getOriginalDuration());
		Assertions.assertFalse(timedEffect.isComplete());
		Assertions.assertFalse(timedEffect.isPaused());
		Assertions.assertFalse(timedEffect.hasStarted());
		Assertions.assertTrue(timedEffect.blocks());
		Assertions.assertTrue(timedEffect.waits());

		// void methods
		Assertions.assertThrows(IllegalStateException.class, timedEffect::resume); // throws because effect has not been paused
		Assertions.assertThrows(IllegalStateException.class, timedEffect::pause); // throws because effect has not started
		Assertions.assertThrows(IllegalStateException.class, timedEffect::complete); // will throw because effect has not started
		Assertions.assertThrows(IllegalStateException.class, () -> timedEffect.complete(true)); // will throw because effect has not started
		Assertions.assertThrows(IllegalStateException.class, () -> timedEffect.complete(false)); // will throw because effect has not started

		TimedEffect newEffect = timedEffect.toBuilder().effectGroup("blah").build();
		Assertions.assertEquals("blah", newEffect.getEffectGroup());
	}

	@Test
	public void builderTests() {
		TimedEffect.Builder builder = new TimedEffect.Builder();

		Assertions.assertNull(builder.duration());
		builder.duration(1000);
		Assertions.assertEquals(Duration.ofSeconds(1), builder.duration());
		builder.duration(null);
		Assertions.assertNull(builder.duration());
		builder.duration(Duration.ofSeconds(2));
		Assertions.assertEquals(Duration.ofSeconds(2), builder.duration());
		builder.duration(TimeUnit.SECONDS, 3);
		Assertions.assertEquals(Duration.ofSeconds(3), builder.duration());
		builder.duration(ChronoUnit.SECONDS, 4);
		Assertions.assertThrows(IllegalArgumentException.class, () -> builder.duration((TimeUnit) null, 5));
		Assertions.assertThrows(IllegalArgumentException.class, () -> builder.duration((ChronoUnit) null, 5));
		Assertions.assertEquals(Duration.ofSeconds(4), builder.duration());

		Assertions.assertNull(builder.request());
		Request request = new Request.Builder().id(1).effect("test").viewer("test").duration(Duration.ofSeconds(5)).build();
		builder.request(request);
		Assertions.assertEquals(request, builder.request());

		Assertions.assertNull(builder.effectGroup());
		builder.effectGroup("test2");
		Assertions.assertEquals("test2", builder.effectGroup());

		Assertions.assertNull(builder.startCallback());
		Response.Builder response = request.buildResponse();
		Function<TimedEffect, Response.Builder> callback = $ -> response;
		builder.startCallback(callback);
		Assertions.assertNotNull(builder.startCallback());
		Assertions.assertEquals(response, builder.startCallback().apply(null));

		Assertions.assertNull(builder.pauseCallback());
		builder.pauseCallback($ -> {
		});
		Assertions.assertNotNull(builder.pauseCallback());

		Assertions.assertNull(builder.resumeCallback());
		builder.resumeCallback($ -> {
		});
		Assertions.assertNotNull(builder.resumeCallback());

		Assertions.assertNull(builder.completionCallback());
		builder.completionCallback($ -> {
		});
		Assertions.assertNotNull(builder.completionCallback());

		Assertions.assertTrue(builder.blocks());
		builder.blocks(false);
		Assertions.assertFalse(builder.blocks());

		Assertions.assertTrue(builder.waits());
		builder.waits(false);
		Assertions.assertFalse(builder.waits());

		// build & test getters
		TimedEffect timedEffect = builder.clone().build().toBuilder().build();
		Assertions.assertEquals(request, timedEffect.getRequest());
		Assertions.assertEquals("test2", timedEffect.getEffectGroup());
		Assertions.assertEquals(Duration.ofSeconds(4), timedEffect.getOriginalDuration());
		Assertions.assertEquals(Duration.ofSeconds(4), timedEffect.getCurrentDuration());
		Assertions.assertFalse(timedEffect.isComplete());
		Assertions.assertFalse(timedEffect.isPaused());
		Assertions.assertFalse(timedEffect.hasStarted());
		Assertions.assertFalse(timedEffect.blocks());
		Assertions.assertFalse(timedEffect.waits());

		// misc
		timedEffect = timedEffect.toBuilder().effectGroup(null).duration(null).build();
		Assertions.assertEquals("test", timedEffect.getEffectGroup());
		Assertions.assertEquals(Duration.ofSeconds(5), timedEffect.getOriginalDuration());
		Assertions.assertEquals(Duration.ofSeconds(5), timedEffect.getCurrentDuration());
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void staticMethodTests() throws InterruptedException {
		// because of Race Conditions(tm) this test will sometimes fail
		// this busy loop ensures that it will not fail
		int delay = 1;
		while (TimedEffect.isActive(request) && delay <= 14) {
			Thread.sleep((long) Math.pow(2, delay++));
		}

		Assertions.assertFalse(TimedEffect.isActive(request));
		Assertions.assertFalse(TimedEffect.isActive("blah"));
		Assertions.assertFalse(TimedEffect.isActive("blah", request));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null, (Request) null));
		Assertions.assertDoesNotThrow(() -> TimedEffect.isActive(null, (Request.Target) null));
		Assertions.assertDoesNotThrow(() -> TimedEffect.isActive(null, (Request.Target[]) null));
	}
}
