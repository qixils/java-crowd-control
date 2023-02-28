package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNullableByDefault;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A wrapper for safely executing exclusive timed effects.
 * Effects with the same {@link #getEffectGroup() key} and {@link Request#getTargets() target}
 * will run one after another.
 * <p>
 * To enqueue this effect, execute {@link #queue()} after instantiating a new object.
 * Similarly, the effect can be {@link #pause() paused}, {@link #resume() resumed},
 * or {@link #complete() stopped}.
 * </p>
 * This class can be constructed via {@link Builder}.
 *
 * @since 2.1.0
 */
@ApiStatus.AvailableSince("2.1.0")
public final class TimedEffect {

	private static final Map<MapKey, TimedEffect> ACTIVE_EFFECTS = new HashMap<>();
	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	private static final Logger logger = LoggerFactory.getLogger("CC-TimedEffect");
	private final @NotNull Request request;
	private final @NotNull MapKey globalKey;
	private final MapKey @NotNull [] mapKeys;
	private final @NotNull String effectGroup;
	private final @NotNull Function<@NotNull TimedEffect, Response.@Nullable Builder> callback;
	private final @Nullable Consumer<@NotNull TimedEffect> pauseCallback;
	private final @Nullable Consumer<@NotNull TimedEffect> resumeCallback;
	private final @Nullable Consumer<@NotNull TimedEffect> completionCallback;
	private final long originalDuration;
	private final boolean waitsForOthers;
	private final boolean blocksOthers;
	private long startedAt = -1;
	private long duration;
	private boolean paused = false;
	private boolean queued = false;
	private @Nullable ScheduledFuture<?> future;

	private TimedEffect(@NotNull Request request,
						@Nullable String effectGroup,
						@Nullable Duration duration,
						@NotNull Function<@NotNull TimedEffect, Response.@Nullable Builder> callback,
						@Nullable Consumer<@NotNull TimedEffect> pauseCallback,
						@Nullable Consumer<@NotNull TimedEffect> resumeCallback,
						@Nullable Consumer<@NotNull TimedEffect> completionCallback,
						boolean waitsForOthers,
						boolean blocksOthers) throws IllegalArgumentException {
		this.request = ExceptionUtil.validateNotNull(request, "request");
		this.effectGroup = ExceptionUtil.validateNotNullElseGet(effectGroup, request::getEffect);
		this.globalKey = new MapKey(this.effectGroup);
		if (duration != null)
			this.duration = duration.toMillis();
		else if (request.getDuration() != null)
			this.duration = request.getDuration().toMillis();
		else
			throw new IllegalArgumentException("duration must be specified");
		if (this.duration < 0)
			throw new IllegalArgumentException("duration must not be negative");
		this.originalDuration = this.duration;
		this.callback = ExceptionUtil.validateNotNull(callback, "callback");
		this.pauseCallback = pauseCallback;
		this.resumeCallback = resumeCallback;
		this.completionCallback = completionCallback;
		this.waitsForOthers = waitsForOthers;
		this.blocksOthers = blocksOthers;

		Request.Target[] targets = request.getTargets();
		mapKeys = new MapKey[targets.length];
		for (int i = 0; i < targets.length; i++) {
			mapKeys[i] = new MapKey(this.effectGroup, targets[i]);
		}
	}

	@SuppressWarnings("ConstantConditions") // the other constructor will check for null
	private TimedEffect(@NotNull Builder builder) {
		this(builder.request, builder.effectGroup, builder.duration, builder.callback, builder.pauseCallback, builder.resumeCallback, builder.completionCallback, builder.waitsForOthers, builder.blocksOthers);
	}

	/**
	 * Determines if a blocking effect with the provided name is currently active for any of the
	 * provided streamers. An empty array will be interpreted as a global effect.
	 *
	 * @param effectGroup effect group
	 * @param targets     targeted streamers
	 * @return whether the effect is active
	 * @since 2.1.3
	 */
	@ApiStatus.AvailableSince("2.1.3")
	@CheckReturnValue
	public static boolean isActive(@Nullable String effectGroup, Request.Target @Nullable ... targets) {
		if (effectGroup == null)
			return false;
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
	 * Determines if a blocking effect with the provided name is currently active for any streamer
	 * targeted by the provided {@link Request}.
	 *
	 * @param effectGroup effect group
	 * @param request     request to query for targeted streamers
	 * @return whether the effect is active
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@CheckReturnValue
	public static boolean isActive(@NotNull String effectGroup, @NotNull Request request) {
		return isActive(effectGroup, ExceptionUtil.validateNotNull(request, "request").getTargets());
	}

	/**
	 * Determines if the given {@link Request} is active for any targeted streamer.
	 *
	 * @param request requested effect
	 * @return whether the effect is active
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@CheckReturnValue
	public static boolean isActive(@NotNull Request request) {
		ExceptionUtil.validateNotNull(request, "request");
		return isActive(request.getEffect(), request.getTargets());
	}

	/**
	 * Returns the current time remaining in the effect.
	 * If the effect has completed, this will return {@link Duration#ZERO zero}.
	 * If the effect has not started, this will return the original duration set upon construction.
	 *
	 * @return time left
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	@CheckReturnValue
	@NotNull
	public Duration getCurrentDuration() {
		final long value;
		if (duration == -1)
			value = 0;
		else if (startedAt == -1)
			value = originalDuration;
		else
			value = Math.max(0, duration - (System.currentTimeMillis() - startedAt));
		return Duration.ofMillis(value);
	}

	/**
	 * Queues this effect for execution. Timed effects with the same {@link #getEffectGroup() key}
	 * and {@link Request#getTargets() target} will run one at a time.
	 *
	 * @throws IllegalStateException the effect has already {@link #hasStarted() started} or was already queued
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	public void queue() throws IllegalStateException {
		if (queued)
			throw new IllegalStateException("Effect was already queued");
		queued = true;

		// force start if it doesn't wait for others
		if (!waitsForOthers) {
			start();
			return;
		}

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
		// add to active effects if it blocks others
		if (blocksOthers) {
			if (mapKeys.length == 0)
				ACTIVE_EFFECTS.put(globalKey, this);
			else {
				for (MapKey mapKey : mapKeys)
					ACTIVE_EFFECTS.put(mapKey, this);
			}
		}
		// update vars and run callback
		startedAt = System.currentTimeMillis();
		duration = originalDuration;
		Response.Builder response;
		try {
			response = callback.apply(this);
		} catch (Throwable exception) {
			logger.error("Exception occurred during starting callback", exception);
			request.buildResponse().type(Response.ResultType.FAILURE).message("Requested effect failed to execute").send();

			duration = -1;
			if (blocksOthers) {
				if (mapKeys.length == 0)
					ACTIVE_EFFECTS.remove(globalKey, this);
				else {
					for (MapKey mapKey : mapKeys)
						ACTIVE_EFFECTS.remove(mapKey, this);
				}
			}
			return;
		}

		if (response == null)
			response = request.buildResponse().type(Response.ResultType.SUCCESS);
		else if (response.type() == null)
			response.type(Response.ResultType.SUCCESS);

		if (response.type() == Response.ResultType.SUCCESS) {
			response.timeRemaining(duration);
			future = EXECUTOR.schedule(() -> complete(), duration, TimeUnit.MILLISECONDS);
		}

		response.send();
	}

	/**
	 * Indicates that the effect has paused execution.
	 *
	 * @throws IllegalStateException the effect is already paused, has not {@link #hasStarted() started}, or has {@link #isComplete() completed}
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	public void pause() throws IllegalStateException { // TODO: change to boolean return in major version
		if (future == null || startedAt == -1)
			throw new IllegalStateException("Effect has not started");
		if (paused)
			throw new IllegalStateException("Effect is already paused");
		if (duration == -1)
			throw new IllegalStateException("Effect has already completed");

		duration = getCurrentDuration().toMillis();
		if (duration <= 0)
			throw new IllegalStateException("Effect has already completed");

		future.cancel(false);

		if (pauseCallback != null) {
			try {
				pauseCallback.accept(this);
			} catch (Exception e) {
				logger.error("Exception occurred during pause callback", e);
			}
		}

		paused = true;
		request.buildResponse().type(Response.ResultType.PAUSED).timeRemaining(duration).send();
	}

	/**
	 * Indicates that the effect has resumed execution.
	 *
	 * @throws IllegalStateException the effect is not paused, has not {@link #hasStarted() started}, or has {@link #isComplete() completed}
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	public void resume() throws IllegalStateException {
		if (!paused)
			throw new IllegalStateException("Effect was not paused");
		if (duration <= 0)
			throw new IllegalStateException("Effect has already completed");
		if (startedAt == -1)
			throw new IllegalStateException("Effect has not started");

		if (resumeCallback != null) {
			try {
				resumeCallback.accept(this);
			} catch (Exception e) {
				logger.error("Exception occurred during resume callback", e);
			}
		}

		paused = false;
		startedAt = System.currentTimeMillis();
		request.buildResponse().type(Response.ResultType.RESUMED).timeRemaining(duration).send();
		future = EXECUTOR.schedule(() -> complete(), duration, TimeUnit.MILLISECONDS);
	}

	/**
	 * Marks the effect as completed and executes the
	 * {@link Builder#completionCallback() completion callback}.
	 *
	 * @return Whether the effect was marked as complete as a result of this method call.
	 * If this effect was already complete, {@code false} is returned.
	 * @throws IllegalStateException the effect has not {@link #hasStarted() started}
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	public boolean complete() throws IllegalStateException {
		return complete(true);
	}

	/**
	 * Marks the effect as completed and optionally executes the
	 * {@link Builder#completionCallback() completion callback}.
	 *
	 * @param executeCompletionCallback whether to execute the
	 *                                  {@link Builder#completionCallback() completion callback}
	 * @return Whether the effect was marked as complete as a result of this method call.
	 * If this effect was already complete, {@code false} is returned.
	 * @throws IllegalStateException the effect has not {@link #hasStarted() started}
	 * @since 3.3.3
	 */
	@ApiStatus.AvailableSince("3.3.3")
	public boolean complete(boolean executeCompletionCallback) throws IllegalStateException {
		if (startedAt == -1)
			throw new IllegalStateException("Effect has not started");
		if (duration == -1)
			return false;
		duration = -1;

		if (blocksOthers) {
			if (mapKeys.length == 0)
				ACTIVE_EFFECTS.remove(globalKey, this);
			else {
				for (MapKey mapKey : mapKeys)
					ACTIVE_EFFECTS.remove(mapKey, this);
			}
		}

		request.buildResponse().type(Response.ResultType.FINISHED).send();
		if (executeCompletionCallback && completionCallback != null) {
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
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	@CheckReturnValue
	public boolean isComplete() {
		return getCurrentDuration().isZero();
	}

	/**
	 * Determines if this effect has started.
	 *
	 * @return start status
	 * @since 2.1.0
	 */
	@ApiStatus.AvailableSince("2.1.0")
	@CheckReturnValue
	public boolean hasStarted() {
		return startedAt != -1;
	}

	/**
	 * Gets the request that this timed effect corresponds to.
	 *
	 * @return request ID
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@NotNull
	@CheckReturnValue
	public Request getRequest() {
		return request;
	}

	/**
	 * Gets the effect group which this effect will be queued in.
	 *
	 * @return effect group
	 * @since 3.0.0
	 */
	@ApiStatus.AvailableSince("3.0.0")
	@NotNull
	@CheckReturnValue
	public String getEffectGroup() {
		return effectGroup;
	}

	/**
	 * Gets the duration of that was set on construction.
	 *
	 * @return original duration
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@CheckReturnValue
	@NotNull
	public Duration getOriginalDuration() {
		return Duration.ofMillis(originalDuration);
	}

	/**
	 * Determines if the effect is paused.
	 *
	 * @return paused status
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@CheckReturnValue
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Whether this effect blocks other effects from being executed
	 * (provided those other effects are in the same effect group and {@link #waits() waits for others}).
	 *
	 * @return whether this effect blocks others
	 * @since 3.5.0
	 */
	@ApiStatus.AvailableSince("3.5.0")
	@CheckReturnValue
	public boolean blocks() {
		return blocksOthers;
	}

	/**
	 * Whether this effect waits for other effects in the same effect group to complete.
	 *
	 * @return whether this effect waits for others
	 * @see #blocks()
	 * @since 3.5.0
	 */
	@ApiStatus.AvailableSince("3.5.0")
	@CheckReturnValue
	public boolean waits() {
		return waitsForOthers;
	}

	/**
	 * Creates a mutable {@link Builder} with a copy of the data in this {@link TimedEffect}.
	 *
	 * @return a new {@link Builder}
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@NotNull
	@CheckReturnValue
	@Contract(" -> new")
	public Builder toBuilder() {
		return new Builder(this);
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

	/**
	 * Builds a new {@link TimedEffect}, a wrapper for safely executing exclusive timed effects.
	 *
	 * @see TimedEffect
	 * @since 3.3.2
	 */
	@ApiStatus.AvailableSince("3.3.2")
	@SuppressWarnings("UnusedReturnValue") // used in method chaining
	@ParametersAreNullableByDefault
	public static final class Builder implements Cloneable {
		private @Nullable Request request;
		private @Nullable String effectGroup;
		private @Nullable Function<@NotNull TimedEffect, Response.@Nullable Builder> callback;
		private @Nullable Consumer<@NotNull TimedEffect> pauseCallback;
		private @Nullable Consumer<@NotNull TimedEffect> resumeCallback;
		private @Nullable Consumer<@NotNull TimedEffect> completionCallback;
		private @Nullable Duration duration;
		private boolean blocksOthers = true;
		private boolean waitsForOthers = true;

		/**
		 * Creates a new {@link TimedEffect.Builder}.
		 *
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		public Builder() {
		}

		/**
		 * Creates a copy of the given {@link TimedEffect.Builder}.
		 *
		 * @param builder the builder to copy
		 * @throws IllegalArgumentException if the given builder is {@code null}
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		private Builder(@NotNull Builder builder) {
			ExceptionUtil.validateNotNull(builder, "builder");
			this.request = builder.request;
			this.effectGroup = builder.effectGroup;
			this.callback = builder.callback;
			this.pauseCallback = builder.pauseCallback;
			this.resumeCallback = builder.resumeCallback;
			this.completionCallback = builder.completionCallback;
			this.duration = builder.duration;
			this.blocksOthers = builder.blocksOthers;
			this.waitsForOthers = builder.waitsForOthers;
		}

		/**
		 * Creates a new {@link TimedEffect.Builder} from the provided {@link TimedEffect}.
		 *
		 * @param effect the timed effect to copy
		 * @throws IllegalArgumentException if the given effect is {@code null}
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		private Builder(@NotNull TimedEffect effect) {
			ExceptionUtil.validateNotNull(effect, "effect");
			this.request = effect.request;
			this.effectGroup = effect.effectGroup;
			this.callback = effect.callback;
			this.pauseCallback = effect.pauseCallback;
			this.resumeCallback = effect.resumeCallback;
			this.completionCallback = effect.completionCallback;
			this.duration = effect.getOriginalDuration();
			this.blocksOthers = effect.blocksOthers;
			this.waitsForOthers = effect.waitsForOthers;
		}

		/**
		 * Sets the request that this timed effect corresponds to.
		 *
		 * @param request originating request
		 * @return this builder
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder request(@Nullable Request request) {
			this.request = request;
			return this;
		}

		/**
		 * Sets the effect group which this effect will be queued in.
		 *
		 * @param effectGroup effect group
		 * @return this builder
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder effectGroup(@Nullable String effectGroup) {
			this.effectGroup = effectGroup;
			return this;
		}

		/**
		 * Sets the callback that will be executed when the effect starts.
		 * <p>
		 * If the callback returns {@code null}, it will be interpreted as a successful completion.
		 * This behavior is not recommended and will be removed in a future version. Instead, you
		 * should return the builder, as it defaults to a successful completion.
		 *
		 * @param callback callback to execute
		 * @return this builder
		 * @see #legacyStartCallback(Consumer)
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder startCallback(@Nullable Function<@NotNull TimedEffect, Response.@Nullable Builder> callback) {
			this.callback = callback;
			return this;
		}

		/**
		 * Sets the callback that will be executed when the effect starts.
		 * The callback is assumed to be always successful.
		 *
		 * @param callback callback to execute
		 * @return this builder
		 * @see #startCallback(Function)
		 * @since 3.3.2
		 * @deprecated use {@link #startCallback(Function)} instead
		 */
		@Deprecated
		@ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder legacyStartCallback(@Nullable Consumer<@NotNull TimedEffect> callback) {
			if (callback == null)
				this.callback = null;
			else
				this.callback = effect -> {
					callback.accept(effect);
					return null;
				};

			return this;
		}

		/**
		 * Sets the callback that will be executed when the effect is paused.
		 *
		 * @param callback callback to execute
		 * @return this builder
		 * @since 3.5.3
		 */
		@ApiStatus.AvailableSince("3.5.3")
		@NotNull
		@Contract("_ -> this")
		public Builder pauseCallback(@Nullable Consumer<@NotNull TimedEffect> callback) {
			this.pauseCallback = callback;
			return this;
		}

		/**
		 * Sets the callback that will be executed when the effect is resumed.
		 *
		 * @param callback callback to execute
		 * @return this builder
		 * @since 3.5.3
		 */
		@ApiStatus.AvailableSince("3.5.3")
		@NotNull
		@Contract("_ -> this")
		public Builder resumeCallback(@Nullable Consumer<@NotNull TimedEffect> callback) {
			this.resumeCallback = callback;
			return this;
		}

		/**
		 * Sets the callback that will be executed if and when the effect completes.
		 *
		 * @param completionCallback callback to execute upon completion
		 * @return this builder
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder completionCallback(@Nullable Consumer<@NotNull TimedEffect> completionCallback) {
			this.completionCallback = completionCallback;
			return this;
		}

		/**
		 * Sets the duration of this effect.
		 *
		 * @param millis duration in milliseconds
		 * @return this builder
		 * @see #duration(Duration)
		 * @see #duration(TimeUnit, long)
		 * @see #duration(TemporalUnit, long)
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder duration(long millis) {
			this.duration = Duration.ofMillis(millis);
			return this;
		}

		/**
		 * Sets the duration of this effect.
		 *
		 * @param duration duration
		 * @return this builder
		 * @see #duration(long)
		 * @see #duration(TimeUnit, long)
		 * @see #duration(TemporalUnit, long)
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_ -> this")
		public Builder duration(@Nullable Duration duration) {
			this.duration = duration;
			return this;
		}

		/**
		 * Sets the duration of this effect.
		 *
		 * @param unit     duration unit
		 * @param duration duration amount
		 * @return this builder
		 * @throws IllegalArgumentException if the unit is null
		 * @see #duration(long)
		 * @see #duration(Duration)
		 * @see #duration(TemporalUnit, long)
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract("_, _ -> this")
		public Builder duration(@NotNull TimeUnit unit, long duration) throws IllegalArgumentException {
			ExceptionUtil.validateNotNull(unit, "unit");
			this.duration = Duration.ofMillis(unit.toMillis(duration));
			return this;
		}

		/**
		 * Sets the duration of this effect.
		 *
		 * @param unit     duration unit
		 * @param duration duration amount
		 * @return this builder
		 * @throws IllegalArgumentException if the unit is null
		 * @see #duration(long)
		 * @see #duration(Duration)
		 * @see #duration(TimeUnit, long)
		 * @since 3.5.0
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_, _ -> this")
		public Builder duration(@NotNull TemporalUnit unit, long duration) throws IllegalArgumentException {
			ExceptionUtil.validateNotNull(unit, "unit");
			this.duration = Duration.of(duration, unit);
			return this;
		}

		/**
		 * Sets this effect blocks other effects from being executed
		 * (provided those other effects are in the same effect group and {@link #waits() waits for others}).
		 *
		 * @param blocks whether this effect blocks other effects
		 * @return this builder
		 * @since 3.5.0
		 * @see #waits(boolean)
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_ -> this")
		public Builder blocks(boolean blocks) {
			this.blocksOthers = blocks;
			return this;
		}

		/**
		 * Sets whether this effect waits for other effects in the same effect group to complete.
		 *
		 * @param waits whether this effect waits for others
		 * @return this builder
		 * @since 3.5.0
		 * @see #blocks(boolean)
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@NotNull
		@Contract("_ -> this")
		public Builder waits(boolean waits) {
			this.waitsForOthers = waits;
			return this;
		}


		// getters

		/**
		 * Gets the request that caused this timed effect to be created.
		 * May be {@code null} if not yet set.
		 *
		 * @return originating request
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@Nullable
		@Contract(pure = true)
		@CheckReturnValue
		public Request request() {
			return request;
		}

		/**
		 * Gets the effect group which this effect will be queued in.
		 * <p>
		 * <b>Note:</b> unlike a built {@link TimedEffect}, this value will return {@code null} if
		 * unset rather than the result of {@link Request#getEffect()}.
		 *
		 * @return effect group
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@Nullable
		@Contract(pure = true)
		@CheckReturnValue
		public String effectGroup() {
			return effectGroup;
		}

		/**
		 * Gets the callback that will be executed when the effect starts.
		 * May be {@code null} if not yet set.
		 *
		 * @return callback to execute
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@Nullable
		@Contract(pure = true)
		@CheckReturnValue
		public Function<@NotNull TimedEffect, Response.@Nullable Builder> startCallback() {
			return callback;
		}

		/**
		 * Gets the callback that will be executed if and when the effect completes.
		 * May be {@code null} if not yet set.
		 *
		 * @return callback to execute upon completion
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@Nullable
		@Contract(pure = true)
		@CheckReturnValue
		public Consumer<@NotNull TimedEffect> completionCallback() {
			return completionCallback;
		}

		/**
		 * Gets the duration of this effect.
		 * May be {@code null} if not yet set.
		 *
		 * @return duration in milliseconds
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@Contract(pure = true)
		@CheckReturnValue
		@Nullable
		public Duration duration() {
			return duration;
		}

		/**
		 * Gets whether this effect blocks other effects from being executed
		 * (provided those other effects are in the same effect group and {@link #waits() waits for others}).
		 *
		 * @return whether this effect blocks other effects
		 * @since 3.5.0
		 * @see #waits()
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@Contract(pure = true)
		@CheckReturnValue
		public boolean blocks() {
			return blocksOthers;
		}

		/**
		 * Gets whether this effect waits for other effects in the same effect group to complete.
		 *
		 * @return whether this effect waits for others
		 * @since 3.5.0
		 * @see #blocks()
		 */
		@ApiStatus.AvailableSince("3.5.0")
		@Contract(pure = true)
		@CheckReturnValue
		public boolean waits() {
			return waitsForOthers;
		}


		// build

		/**
		 * Builds a new timed effect.
		 *
		 * @return new timed effect
		 * @throws IllegalStateException may be thrown in various circumstances:
		 *                               <ul>
		 *                                   <li>if the {@link #request() request} is null</li>
		 *                                   <li>if the {@link #duration() duration} is negative</li>
		 *                                   <li>if the {@link #startCallback() callback} is null</li>
		 *                               </ul>
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@NotNull
		@Contract(pure = true)
		@CheckReturnValue
		public TimedEffect build() throws IllegalStateException {
			return new TimedEffect(this);
		}

		/**
		 * Creates a cloned copy of this builder.
		 *
		 * @return new builder
		 * @since 3.3.2
		 */
		@ApiStatus.AvailableSince("3.3.2")
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		@Contract(" -> new")
		public Builder clone() {
			return new Builder(this);
		}
	}
}
