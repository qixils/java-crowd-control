package dev.qixils.crowdcontrol;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Subscribes a method to listen to effects. The containing class must be {@link CrowdControl#registerHandlers(Object) registered}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
	/**
	 * The name of the effect to listen to.
	 * @return effect name
	 */
	String effect();
}
