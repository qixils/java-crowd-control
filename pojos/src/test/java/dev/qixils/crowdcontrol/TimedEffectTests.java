package dev.qixils.crowdcontrol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Miscellaneous tests for the TimedEffect class that can be run in isolation.
 */
@SuppressWarnings("ConstantConditions")
public class TimedEffectTests {
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


	}
}
