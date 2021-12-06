package dev.qixils.crowdcontrol;

import java.util.function.Function;

/**
 * The value returned by a {@link CrowdControl#registerCheck(Function) check}.
 */
public enum CheckResult {
	/**
	 * Allows an effect to be executed, assuming all other checks return the same result.
	 */
	ALLOW,
	/**
	 * Prevents an effect from being executed.
	 */
	DISALLOW
}
