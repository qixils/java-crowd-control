package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.socket.Request;
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
 * Effects with the same {@link #getEffect() key} will run one after another.
 * <p>
 * To enqueue this effect, execute {@link #queue()} after instantiating a new object.
 * Similarly, the effect can be {@link #pause() paused}, {@link #resume() resumed}, or {@link #complete() stopped}.
 */
public final class TimedEffect {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, TimedEffect> ACTIVE_EFFECTS = new HashMap<>();
    private static final Logger logger = Logger.getLogger("CC-TimedEffect");

    private long startedAt = -1;
    private long duration;
    private boolean paused = false;
    private boolean queued = false;
    private final CrowdControl cc;
    private final String effect;
    private final int id;
    private final @NotNull Consumer<TimedEffect> callback;
    private final @Nullable Consumer<TimedEffect> completionCallback;

    /**
     * Creates a new {@link TimedEffect}.
     * @param ccAPI Crowd Control API
     * @param requestId ID of the {@link Request} that triggered this effect
     * @param effect name of this effect, or of a grouped queue
     * @param duration duration of the effect in milliseconds
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull CrowdControl ccAPI, int requestId, @NotNull String effect, long duration, @NotNull Consumer<TimedEffect> callback, @Nullable Consumer<TimedEffect> completionCallback) {
        this.cc = Objects.requireNonNull(ccAPI, "ccAPI cannot be null");
        this.callback = Objects.requireNonNull(callback, "callback cannot be null");
        this.completionCallback = completionCallback;
        this.duration = duration;
        this.effect = Objects.requireNonNull(effect, "effect cannot be null");
        this.id = requestId;
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param ccAPI Crowd Control API
     * @param request {@link Request} that triggered this effect
     * @param duration duration of the effect in milliseconds
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull CrowdControl ccAPI, @NotNull Request request, long duration, @NotNull Consumer<TimedEffect> callback, @Nullable Consumer<TimedEffect> completionCallback) {
        this(ccAPI, Objects.requireNonNull(request, "request cannot be null").getId(), request.getEffect(), duration, callback, completionCallback);
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param ccAPI Crowd Control API
     * @param requestId ID of the {@link Request} that triggered this effect
     * @param effect name of this effect, or of a grouped queue
     * @param duration duration of the effect
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull CrowdControl ccAPI, int requestId, @NotNull String effect, @NotNull Duration duration, @NotNull Consumer<TimedEffect> callback, @Nullable Consumer<TimedEffect> completionCallback) {
        this(ccAPI, requestId, effect, Objects.requireNonNull(duration, "duration cannot be null").toMillis(), callback, completionCallback);
    }

    /**
     * Creates a new {@link TimedEffect}.
     * @param ccAPI Crowd Control API
     * @param request request that triggered this effect
     * @param duration duration of the effect
     * @param callback method to call once the effect is started
     * @param completionCallback optional method to call once the effect is completed
     */
    @CheckReturnValue
    public TimedEffect(@NotNull CrowdControl ccAPI, @NotNull Request request, @NotNull Duration duration, @NotNull Consumer<TimedEffect> callback, @Nullable Consumer<TimedEffect> completionCallback) {
        this(ccAPI, request, Objects.requireNonNull(duration, "duration cannot be null").toMillis(), callback, completionCallback);
    }

    /**
     * Returns the current milliseconds remaining in the effect, or 0 if it should be complete.
     * @return time left in milliseconds
     */
    @CheckReturnValue
    public long getCurrentDuration() {
        return Math.max(0, duration - (System.currentTimeMillis() - startedAt));
    }

    /**
     * Queues this effect for execution. Timed effects with the same {@link #getEffect() key} will run one at a time.
     * @throws IllegalStateException the effect has already {@link #hasStarted() started} or was already queued
     */
    public void queue() throws IllegalStateException {
        if (queued)
            throw new IllegalStateException("Effect was already queued");
        queued = true;

        TimedEffect activeEffect = ACTIVE_EFFECTS.get(effect);

        if (activeEffect == null || activeEffect.isComplete()) {
            start();
            return;
        }

        cc.dispatchResponse(Response.builder().id(id).type(Response.ResultType.RETRY).message("Timed effect is already running").build());
    }

    private void start() {
        ACTIVE_EFFECTS.put(effect, this);
        startedAt = System.currentTimeMillis();
        cc.dispatchResponse(Response.builder().id(id).type(Response.ResultType.SUCCESS).timeRemaining(duration).build());
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
        cc.dispatchResponse(Response.builder().id(id).type(Response.ResultType.PAUSED).timeRemaining(duration).build());
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
        cc.dispatchResponse(Response.builder().id(id).type(Response.ResultType.RESUMED).timeRemaining(duration).build());
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
        ACTIVE_EFFECTS.remove(effect, this); // using "value" param as a failsafe -- not sure if it helps or hurts tbh
        cc.dispatchResponse(Response.builder().id(id).type(Response.ResultType.FINISHED).build());
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
     * Gets the ID of the request that this timed effect corresponds to.
     * @return request ID
     */
    @CheckReturnValue
    public int getId() {
        return id;
    }

    /**
     * Gets the name of the effect.
     * @return effect name
     */
    @CheckReturnValue
    public String getEffect() {
        return effect;
    }
}
