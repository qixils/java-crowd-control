package dev.qixils.crowdcontrol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TriStateTests {
	@Test
	void fromBoolean() {
		Assertions.assertEquals(TriState.TRUE, TriState.fromBoolean(true));
		Assertions.assertEquals(TriState.FALSE, TriState.fromBoolean(false));
		Assertions.assertEquals(TriState.UNKNOWN, TriState.fromBoolean(null));
	}

	@Test
	void getBoolean() {
		Assertions.assertEquals(Boolean.TRUE, TriState.TRUE.getBoolean());
		Assertions.assertEquals(Boolean.FALSE, TriState.FALSE.getBoolean());
		Assertions.assertNull(TriState.UNKNOWN.getBoolean());
	}

	@Test
	void getPrimitiveBoolean() {
		Assertions.assertTrue(TriState.TRUE.getPrimitiveBoolean());
		Assertions.assertFalse(TriState.FALSE.getPrimitiveBoolean());
		Assertions.assertFalse(TriState.UNKNOWN.getPrimitiveBoolean());
	}

	@Test
	void values() {
		Assertions.assertEquals(3, TriState.values().length);
	}

	@Test
	void valueOf() {
		Assertions.assertEquals(TriState.TRUE, TriState.valueOf("TRUE"));
		Assertions.assertEquals(TriState.FALSE, TriState.valueOf("FALSE"));
		Assertions.assertEquals(TriState.UNKNOWN, TriState.valueOf("UNKNOWN"));
	}
}
