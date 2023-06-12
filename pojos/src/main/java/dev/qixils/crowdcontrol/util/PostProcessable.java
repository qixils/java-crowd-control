package dev.qixils.crowdcontrol.util;

import org.jetbrains.annotations.ApiStatus;

/**
 * An object that can be post-processed after GSON deserialization.
 *
 * @since 3.6.2
 */
@ApiStatus.Internal
public interface PostProcessable {

	/**
	 * Post-processes this object.
	 * For internal use only.
	 *
	 * @since 3.6.2
	 */
	@ApiStatus.Internal
	void postProcess();
}
