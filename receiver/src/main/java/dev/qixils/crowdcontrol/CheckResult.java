package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * The value returned by a {@link CrowdControl#registerCheck(Function) check}.
 *
 * @since 3.2.1
 */
@ApiStatus.AvailableSince("3.2.1")
public enum CheckResult {

	/**
	 * Allows an effect to be executed, assuming all other checks return the same result.
	 *
	 * @since 3.2.1
	 */
	@ApiStatus.AvailableSince("3.2.1")
	ALLOW,

	/**
	 * Prevents an effect from being executed.
	 *
	 * @since 3.2.1
	 */
	@ApiStatus.AvailableSince("3.2.1")
	DISALLOW
}
