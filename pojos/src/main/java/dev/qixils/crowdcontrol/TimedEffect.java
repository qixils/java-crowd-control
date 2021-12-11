package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A wrapper for safely executing exclusive timed effects.
 * Effects with the same {@link #getEffectGroup() key} and {@link Request#getTargets() target}
 * will run one after another.
 * <p>
 * To enqueue this effect, execute {@link #queue()} after instantiating a new object.
 * Similarly, the effect can be {@link #pause() paused}, {@link #resume() resumed},
 * or {@link #complete() stopped}.
 */
public final class TimedEffect {

	private static final Map<MapKey, TimedEffect> ACTIVE_EFFECTS = new HashMap<>();
	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	private static final Logger logger = LoggerFactory.getLogger("CC-TimedEffect");
	private final @NotNull Request request;
	private final @NotNull MapKey globalKey;
	private final MapKey @NotNull [] mapKeys;
	private final @NotNull String effectGroup;
	private final @NotNull Consumer<@NotNull TimedEffect> callback;
	private final @Nullable Consumer<@NotNull TimedEffect> completionCallback;
	private final long originalDuration;
	private long startedAt = -1;
	private long duration;
	private boolean paused = false;
	private boolean queued = false;
	private @Nullable ScheduledFuture<?> future;

	/**
	 * Creates a new {@link TimedEffect}.
	 *
	 * @param request            {@link Request} that triggered this effect
	 * @param duration           duration of the effect in milliseconds
	 * @param callback           method to call once the effect is started
	 * @param completionCallback optional method to call once the effect is completed
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the request is null</li>
	 *                                      <li>if the callback is null</li>
	 *                                      <li>if the duration is less than or equal to 0</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	public TimedEffect(@NotNull Request request,
					   long duration,
					   @NotNull Consumer<@NotNull TimedEffect> callback,
					   @Nullable Consumer<@NotNull TimedEffect> completionCallback) throws IllegalArgumentException {
		this(ExceptionUtil.validateNotNull(request, "request"),
				request.getEffect(), duration, callback, completionCallback);
	}

	/**
	 * Creates a new {@link TimedEffect}.
	 *
	 * @param request            {@link Request} that triggered this effect
	 * @param effectGroup        the group of effects that this effect will be queued under
	 * @param duration           duration of the effect in milliseconds
	 * @param callback           method to call once the effect is started
	 * @param completionCallback optional method to call once the effect is completed
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the request is null</li>
	 *                                      <li>if the callback is null</li>
	 *                                      <li>if the duration is less than or equal to 0</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	public TimedEffect(@NotNull Request request,
					   @Nullable String effectGroup,
					   long duration,
					   @NotNull Consumer<@NotNull TimedEffect> callback,
					   @Nullable Consumer<@NotNull TimedEffect> completionCallback) throws IllegalArgumentException {
		this.callback = ExceptionUtil.validateNotNull(callback, "callback");
		this.completionCallback = completionCallback;
		if (duration <= 0)
			throw new IllegalArgumentException("duration must be positive");
		this.originalDuration = duration;
		this.duration = duration;
		this.request = ExceptionUtil.validateNotNull(request, "request");
		this.effectGroup = Objects.requireNonNullElseGet(effectGroup, request::getEffect);
		this.globalKey = new MapKey(this.effectGroup);

		Request.Target[] targets = request.getTargets();
		mapKeys = new MapKey[targets.length];
		for (int i = 0; i < targets.length; i++) {
			mapKeys[i] = new MapKey(this.effectGroup, targets[i]);
		}
	}

	/**
	 * Creates a new {@link TimedEffect}.
	 *
	 * @param request            request that triggered this effect
	 * @param duration           duration of the effect
	 * @param callback           method to call once the effect is started
	 * @param completionCallback optional method to call once the effect is completed
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the request is null</li>
	 *                                      <li>if the callback is null</li>
	 *                                      <li>if the duration is null or non-positive</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	public TimedEffect(@NotNull Request request,
					   @NotNull Duration duration,
					   @NotNull Consumer<@NotNull TimedEffect> callback,
					   @Nullable Consumer<@NotNull TimedEffect> completionCallback) throws IllegalArgumentException {
		this(ExceptionUtil.validateNotNull(request, "request"),
				request.getEffect(), duration, callback, completionCallback);
	}

	/**
	 * Creates a new {@link TimedEffect}.
	 *
	 * @param request            request that triggered this effect
	 * @param effectGroup        the group of effects that this effect will be queued under
	 * @param duration           duration of the effect
	 * @param callback           method to call once the effect is started
	 * @param completionCallback optional method to call once the effect is completed
	 * @throws IllegalArgumentException If a provided argument is invalid. Specifically:
	 *                                  <ul>
	 *                                      <li>if the request is null</li>
	 *                                      <li>if the callback is null</li>
	 *                                      <li>if the duration is null or non-positive</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	public TimedEffect(@NotNull Request request,
					   @Nullable String effectGroup,
					   @NotNull Duration duration,
					   @NotNull Consumer<@NotNull TimedEffect> callback,
					   @Nullable Consumer<@NotNull TimedEffect> completionCallback) throws IllegalArgumentException {
		this(request, effectGroup, ExceptionUtil.validateNotNull(duration, "duration").toMillis(),
				callback, completionCallback);
	}

	/**
	 * Determines if an effect with the provided name is currently active for any of the
	 * provided streamers. An empty array will be interpreted as a global effect.
	 *
	 * @param effectGroup effect group
	 * @param targets     targeted streamers
	 * @return whether the effect is active
	 */
	public static boolean isActive(@NotNull String effectGroup, Request.Target @Nullable ... targets) {
		ExceptionUtil.validateNotNull(effectGroup, "effectGroup");
		if (targets != null) {
			for (Request.Target target : targets) {
				if (target == null)
					throw new IllegalArgumentException("targets cannot be null");
				MapKey key = new MapKey(effectGroup, target);
				if (ACTIVE_EFFECTS.containsKey(key) && !ACTIVE_EFFECTS.get(key).isComplete())
					return true;
			}
		}

		MapKey globalKey = new MapKey(effectGroup, null);
		return ACTIVE_EFFECTS.containsKey(globalKey) && !ACTIVE_EFFECTS.get(globalKey).isComplete();
	}

	/**
	 * Determines if an effect with the provided name is currently active for any streamer
	 * targeted by the provided {@link Request}.
	 *
	 * @param effectGroup effect group
	 * @param request     request to query for targeted streamers
	 * @return whether the effect is active
	 */
	public static boolean isActive(@NotNull String effectGroup, @NotNull Request request) {
		return isActive(effectGroup, ExceptionUtil.validateNotNull(request, "request").getTargets());
	}

	/**
	 * Determines if the given {@link Request} is active for any targeted streamer.
	 *
	 * @param request requested effect
	 * @return whether the effect is active
	 */
	public static boolean isActive(@NotNull Request request) {
		ExceptionUtil.validateNotNull(request, "request");
		return isActive(request.getEffect(), request.getTargets());
	}

	/**
	 * Returns the current milliseconds remaining in the effect.
	 * If the effect has completed, this will return 0.
	 * If the effect has not started, this will return the original duration set upon construction.
	 *
	 * @return time left in milliseconds
	 */
	@CheckReturnValue
	public long getCurrentDuration() {
		if (duration == -1)
			return 0;
		if (startedAt == -1)
			return originalDuration;
		return Math.max(0, duration - (System.currentTimeMillis() - startedAt));
	}

	/**
	 * Queues this effect for execution. Timed effects with the same {@link #getEffectGroup() key}
	 * and {@link Request#getTargets() target} will run one at a time.
	 *
	 * @throws IllegalStateException the effect has already {@link #hasStarted() started} or was already queued
	 */
	public void queue() throws IllegalStateException {
		if (queued)
			throw new IllegalStateException("Effect was already queued");
		queued = true;

		// check if a global effect is running
		TimedEffect globalActiveEffect = ACTIVE_EFFECTS.get(globalKey);
		if (globalActiveEffect != null && !globalActiveEffect.isComplete()) {
			request.buildResponse().type(Response.ResultType.RETRY).message("Timed effect is already running").send();
			return;
		}

		// check if a per-streamer effect is running (on any targeted streamer)
		for (MapKey mapKey : mapKeys) {
			TimedEffect activeEffect = ACTIVE_EFFECTS.get(mapKey);
			if (activeEffect != null && !activeEffect.isComplete()) {
				request.buildResponse().type(Response.ResultType.RETRY).message("Timed effect is already running").send();
				return;
			}
		}

		// start
		start();
	}

	private void start() {
		if (mapKeys.length == 0)
			ACTIVE_EFFECTS.put(globalKey, this);
		else {
			for (MapKey mapKey : mapKeys)
				ACTIVE_EFFECTS.put(mapKey, this);
		}
		startedAt = System.currentTimeMillis();
		request.buildResponse().type(Response.ResultType.SUCCESS).timeRemaining(duration).send();
		try {
			callback.accept(this);
		} catch (Exception exception) {
			logger.error("Exception occurred during starting callback", exception);
		}
		future = EXECUTOR.schedule(this::complete, duration, TimeUnit.MILLISECONDS);
	}

	/**
	 * Indicates that the effect has paused execution.
	 *
	 * @throws IllegalStateException the effect is already paused, has not {@link #hasStarted() started}, or has {@link #isComplete() completed}
	 */
	public void pause() throws IllegalStateException {
		if (paused)
			throw new IllegalStateException("Effect is already paused");
		if (startedAt == -1)
			throw new IllegalStateException("Effect has not started");
		if (duration == -1)
			throw new IllegalStateException("Effect has already completed");

		duration = getCurrentDuration();
		if (duration <= 0)
			throw new IllegalStateException("Effect has already completed");

		assert future != null;
		future.cancel(false);

		paused = true;
		request.buildResponse().type(Response.ResultType.PAUSED).timeRemaining(duration).send();
	}

	/**
	 * Indicates that the effect has resumed execution.
	 *
	 * @throws IllegalStateException the effect is not paused, has not {@link #hasStarted() started}, or has {@link #isComplete() completed}
	 */
	public void resume() throws IllegalStateException {
		if (!paused)
			throw new IllegalStateException("Effect was not paused");
		if (duration <= 0)
			throw new IllegalStateException("Effect has already completed");
		if (startedAt == -1)
			throw new IllegalStateException("Effect has not started");

		paused = false;
		startedAt = System.currentTimeMillis();
		request.buildResponse().type(Response.ResultType.RESUMED).timeRemaining(duration).send();
		future = EXECUTOR.schedule(this::complete, duration, TimeUnit.MILLISECONDS);
	}

	/**
	 * Marks the effect as completed.
	 *
	 * @return Whether the effect was marked as complete as a result of this method call.
	 * If this effect was already complete, {@code false} is returned.
	 * @throws IllegalStateException the effect has not {@link #hasStarted() started}
	 */
	public boolean complete() throws IllegalStateException {
		if (startedAt == -1)
			throw new IllegalStateException("Effect has not started");
		if (duration == -1)
			return false;
		duration = -1;

		if (mapKeys.length == 0)
			ACTIVE_EFFECTS.remove(globalKey, this);
		else {
			for (MapKey mapKey : mapKeys)
				ACTIVE_EFFECTS.remove(mapKey, this);
		}

		request.buildResponse().type(Response.ResultType.FINISHED).send();
		if (completionCallback != null) {
			try {
				completionCallback.accept(this);
			} catch (Exception exception) {
				logger.error("Exception occurred during completion callback", exception);
			}
		}
		return true;
	}

	// boilerplate

	/**
	 * Determines if this effect has completed.
	 *
	 * @return completion status
	 */
	@CheckReturnValue
	public boolean isComplete() {
		return getCurrentDuration() == 0;
	}

	/**
	 * Determines if this effect has started.
	 *
	 * @return start status
	 */
	@CheckReturnValue
	public boolean hasStarted() {
		return startedAt != -1;
	}

	/**
	 * Gets the request that this timed effect corresponds to.
	 *
	 * @return request ID
	 */
	@NotNull
	@CheckReturnValue
	public Request getRequest() {
		return request;
	}

	/**
	 * Gets the effect group which this effect will be queued in.
	 *
	 * @return effect group
	 */
	@NotNull
	@CheckReturnValue
	public String getEffectGroup() {
		return effectGroup;
	}

	/**
	 * Gets the duration of this effect that was set on construction.
	 *
	 * @return duration in milliseconds
	 */
	@CheckReturnValue
	public long getOriginalDuration() {
		return originalDuration;
	}

	/**
	 * Determines if the effect is paused.
	 *
	 * @return paused status
	 */
	public boolean isPaused() {
		return paused;
	}

	// key class

	private static final class MapKey {
		private final @NotNull String effectGroup;
		private final @Nullable Request.Target target;

		private MapKey(@NotNull String effectGroup) {
			this(effectGroup, null);
		}

		private MapKey(@NotNull String effectGroup, @Nullable Request.Target target) {
			this.effectGroup = effectGroup;
			this.target = target;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MapKey mapKey = (MapKey) o;
			return effectGroup.equals(mapKey.effectGroup) && Objects.equals(target, mapKey.target);
		}

		@Override
		public int hashCode() {
			return Objects.hash(effectGroup, target);
		}
	}
}
