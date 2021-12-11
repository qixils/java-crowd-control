package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Miscellaneous tests for the TimedEffect class that can be run in isolation.
 */
@SuppressWarnings("ConstantConditions")
public class TimedEffectTests {
	private static final Request request = new Request(1,
			Request.Type.START,
			"test",
			"qixils",
			"Hello World!",
			10,
			new Request.Target[]{new Request.Target(12345, "qixils", "https://i.qixils.dev/favicon.png")}
	);

	@Test
	public void constructorTest() {
		// Constructor 1

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				null,
				1000,
				$ -> {
				},
				null
		));
		// null callback throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				1000,
				null,
				null
		));
		// non-positive duration throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				0,
				$ -> {
				},
				null
		));

		// Constructor 2

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				null,
				"test",
				1000,
				$ -> {
				},
				null
		));
		// null callback throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				"test",
				1000,
				null,
				null
		));
		// non-positive duration throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				"test",
				0,
				$ -> {
				},
				null
		));

		// Constructor 3

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				null,
				Duration.ofSeconds(1),
				$ -> {
				},
				null
		));
		// null callback throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				Duration.ofSeconds(1),
				null,
				null
		));
		// non-positive duration throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				Duration.ZERO,
				$ -> {
				},
				null
		));

		// Constructor 4

		// null request throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				null,
				"test",
				Duration.ofSeconds(1),
				$ -> {
				},
				null
		));
		// null callback throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				"test",
				Duration.ofSeconds(1),
				null,
				null
		));
		// non-positive duration throws IllegalArgumentException
		Assertions.assertThrows(IllegalArgumentException.class, () -> new TimedEffect(
				request,
				"test",
				Duration.ZERO,
				$ -> {
				},
				null
		));
	}

	@Test
	public void methodTests() {
		TimedEffect timedEffect = new TimedEffect(request, 1000, $ -> {
		}, null);

		// getters
		Assertions.assertEquals(request, timedEffect.getRequest());
		Assertions.assertEquals(request.getEffect(), timedEffect.getEffectGroup());
		Assertions.assertEquals(1000, timedEffect.getCurrentDuration());
		Assertions.assertEquals(1000, timedEffect.getOriginalDuration());
		Assertions.assertFalse(timedEffect.isComplete());
		Assertions.assertFalse(timedEffect.isPaused());
		Assertions.assertFalse(timedEffect.hasStarted());

		// void methods
		Assertions.assertThrows(IllegalStateException.class, timedEffect::resume); // throws because effect has not been paused
		Assertions.assertThrows(IllegalStateException.class, timedEffect::pause); // throws because effect has not started
		Assertions.assertThrows(IllegalStateException.class, timedEffect::queue); // will throw because request is invalid
		Assertions.assertThrows(IllegalStateException.class, timedEffect::queue); // will throw because effect is already queued
		Assertions.assertThrows(IllegalStateException.class, timedEffect::complete); // will throw because effect has not started

		timedEffect = new TimedEffect(request, "blah", 1000, $ -> {
		}, null);
		Assertions.assertEquals("blah", timedEffect.getEffectGroup());
	}

	@Test
	public void staticMethodTests() {
		Assertions.assertFalse(TimedEffect.isActive(request));
		Assertions.assertFalse(TimedEffect.isActive("blah"));
		Assertions.assertFalse(TimedEffect.isActive("blah", request));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null, (Request) null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null, (Request.Target) null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> TimedEffect.isActive(null, (Request.Target[]) null));
	}
}
