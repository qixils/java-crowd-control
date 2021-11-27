package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Request.Target;
import dev.qixils.crowdcontrol.socket.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for safely executing exclusive timed effects.
 * Effects with the same {@link #getEffectGroup() key} and {@link Request#getTargets() target}
 * will run one after another.
 * <p>
 * To enqueue this effect, execute {@link #queue()} after instantiating a new object.
 * Similarly, the effect can be {@link #pause() paused}, {@link #resume() resumed}, or {@link #complete() stopped}.
 */
public final class TimedEffect {

    private static final class MapKey {
        private final @NotNull String effectGroup;
        private final @Nullable Target target;

        private MapKey(@NotNull String effectGroup) {
            this(effectGroup, null);
        }

        private MapKey(@NotNull String effectGroup, @Nullable Target target) {
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

    private static final Map<MapKey, TimedEffect> ACTIVE_EFFECTS = new HashMap<>();

    /**
     * Determines if an effect with the provided name is currently active.
     * @param effectGroup effect group
     * @return whether the effect is active
     */
    public static boolean isActive(@NotNull String effectGroup) {
        return isActive(effectGroup, null);
    }

    /**
     * Determines if an effect with the provided name and targeted streamer is currently active.
     * @param effectGroup effect group
     * @param target targeted streamer
     * @return whether the effect is active
     */
    public static boolean isActive(@NotNull String effectGroup, @Nullable Target target) {
        MapKey key = new MapKey(effectGroup, target);
        return ACTIVE_EFFECTS.containsKey(key) && !ACTIVE_EFFECTS.get(key).isComplete();
    }

    // TODO: #isActive(String,Request) and #isActive(String,Target...)

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Logger logger = Logger.getLogger("CC-TimedEffect");

    private long startedAt = -1;
    private long duration;
    private boolean paused = false;
    private boolean queued = false;
    private final @NotNull Request request;
    private final @NotNull MapKey globalKey;
    private final MapKey @NotNull[] mapKeys;
    private final @NotNull String effectGroup;
    private final @NotNull Consumer<@NotNull TimedEffect> callback;
    private final @Nullable Consumer<@NotNull TimedEffect> completionCallback;

    /**
     * Creates a new {@link TimedEffect}.
     * @param request {@link Request} that triggered this effect
     * @param duration duration of the effect in milliseconds
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull Request request, long duration, @NotNull Consumer<@NotNull TimedEffect> callback, @Nullable Consumer<@NotNull TimedEffect> completionCallback) {
        this(request, request.getEffect(), duration, callback, completionCallback);
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param request {@link Request} that triggered this effect
     * @param effectGroup the group of effects that this effect will be queued under
     * @param duration duration of the effect in milliseconds
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull Request request, @NotNull String effectGroup, long duration, @NotNull Consumer<@NotNull TimedEffect> callback, @Nullable Consumer<@NotNull TimedEffect> completionCallback) {
        this.callback = Objects.requireNonNull(callback, "callback cannot be null");
        this.completionCallback = completionCallback;
        this.duration = duration;
        this.request = Objects.requireNonNull(request, "effect cannot be null");
        this.effectGroup = Objects.requireNonNull(effectGroup, "effectGroup cannot be null");
        this.globalKey = new MapKey(effectGroup);

        Target[] targets = request.getTargets();
        mapKeys = new MapKey[targets.length];
        for (int i = 0; i < targets.length; i++) {
            mapKeys[i] = new MapKey(effectGroup, targets[i]);
        }
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param request request that triggered this effect
     * @param duration duration of the effect
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull Request request, @NotNull Duration duration, @NotNull Consumer<@NotNull TimedEffect> callback, @Nullable Consumer<@NotNull TimedEffect> completionCallback) {
        this(request, request.getEffect(), duration, callback, completionCallback);
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param request request that triggered this effect
     * @param effectGroup the group of effects that this effect will be queued under
     * @param duration duration of the effect
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull Request request, @NotNull String effectGroup, @NotNull Duration duration, @NotNull Consumer<@NotNull TimedEffect> callback, @Nullable Consumer<@NotNull TimedEffect> completionCallback) {
        this(request, effectGroup, duration.toMillis(), callback, completionCallback);
    }

    /**
     * Returns the current milliseconds remaining in the effect, or 0 if it should be complete.
     * @return time left in milliseconds
     */
    @CheckReturnValue
    public long getCurrentDuration() {
        if (duration == -1)
            return 0;
        return Math.max(0, duration - (System.currentTimeMillis() - startedAt));
    }

    /**
     * Queues this effect for execution. Timed effects with the same {@link #getEffectGroup() key}
     * and {@link Request#getTargets() target} will run one at a time.
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
            logger.log(Level.WARNING, "Exception occurred during starting callback", exception);
        }
        EXECUTOR.schedule(this::tryComplete, duration, TimeUnit.MILLISECONDS);
    }

    /**
     * Indicates that the effect has paused execution.
     * @throws IllegalStateException the effect is already paused, has not {@link #hasStarted() started}, or has {@link #isComplete() completed}
     */
    public void pause() throws IllegalStateException {
        if (paused)
            throw new IllegalStateException("Effect is already paused");
        startedAt = -1;
        duration = getCurrentDuration();
        if (duration <= 0)
            throw new IllegalStateException("Effect has already completed");
        if (startedAt == -1)
            throw new IllegalStateException("Effect has not started");

        paused = true;
        request.buildResponse().type(Response.ResultType.PAUSED).timeRemaining(duration).send();
    }

    /**
     * Indicates that the effect has resumed execution.
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
        EXECUTOR.schedule(this::tryComplete, duration, TimeUnit.MILLISECONDS);
    }

    /**
     * Marks the effect as completed.
     * @return Whether the effect was marked as complete as a result of this method call.
     *         If this effect was already complete, {@code false} is returned.
     */
    public boolean complete() {
        if (duration == -1) return false;
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
                logger.log(Level.WARNING, "Exception occurred during completion callback", exception);
            }
        }
        return true;
    }

    private void tryComplete() {
        if (getCurrentDuration() == 0)
            complete();
    }

    /**
     * Determines if this effect has completed.
     * @return completion status
     */
    @CheckReturnValue
    public boolean isComplete() {
        return getCurrentDuration() == 0;
    }

    /**
     * Determines if this effect has started.
     * @return start status
     */
    @CheckReturnValue
    public boolean hasStarted() {
        return startedAt != -1;
    }

    // boilerplate

    /**
     * Gets the request that this timed effect corresponds to.
     * @return request ID
     */
    @NotNull
    @CheckReturnValue
    public Request getRequest() {
        return request;
    }

    /**
     * Gets the effect group which this effect will be queued in.
     * @return effect group
     */
    @NotNull
    @CheckReturnValue
    public String getEffectGroup() {
        return effectGroup;
    }
}
