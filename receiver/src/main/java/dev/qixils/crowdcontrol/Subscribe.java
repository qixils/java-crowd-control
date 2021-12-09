package dev.qixils.crowdcontrol;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is an effect handler for the
 * {@link #effect() provided effect}. The containing class must be
 * {@link CrowdControl#registerHandlers(Object) registered}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Subscribe {
	/**
	 * The name of the effect to listen to.
	 *
	 * @return effect name
	 */
	@NotNull
	String effect();
}
