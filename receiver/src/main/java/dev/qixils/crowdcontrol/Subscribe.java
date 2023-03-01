package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is an effect handler for the
 * {@link #value() provided effect}. The containing class must be
 * {@link CrowdControl#registerHandlers(Object) registered}.
 *
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

	/**
	 * The name of the effect to listen to.
	 *
	 * @return effect name
	 * @since 1.0.0
	 * @deprecated use {@link #value()} instead
	 */
	@ApiStatus.AvailableSince("1.0.0")
	@NotNull
	@Deprecated
	@ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
	String effect() default "";

	/**
	 * The name of the effect to listen to.
	 *
	 * @return effect name
	 * @since 3.5.3
	 */
	@ApiStatus.AvailableSince("3.5.3")
	@NotNull
	String value() default ""; // remove default in 4.0.0
}
