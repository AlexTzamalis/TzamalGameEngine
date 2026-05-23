package me.alextzamalis.engine;

/**
 * A simple countdown timer for cooldowns, delays, and repeating events.
 *
 * <p>The timer counts elapsed time until it reaches its configured
 * duration. Once elapsed, {@link #isReady()} returns true. If
 * auto-reset is enabled, the timer resets itself automatically on
 * elapse (useful for repeating fire rates or periodic events).</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * Timer fireRate = new Timer(0.5f);
 * fireRate.setAutoReset(true);
 *
 * // In the update loop:
 * fireRate.update(dt);
 * if (fireRate.isReady()) {
 *     shootBullet();
 * }
 * }</pre>
 *
 * @author Alexandros Tzamalis
 */
public class Timer {

    private final float duration;
    private float elapsed;
    private boolean autoReset;
    private boolean ready;

    /**
     * Creates a timer that counts down from the given duration.
     *
     * @param duration time in seconds until the timer elapses.
     * @throws IllegalArgumentException if duration is non-positive.
     */
    public Timer(float duration) {
        if (duration <= 0f) {
            throw new IllegalArgumentException(
                    "Timer duration must be positive: " + duration);
        }
        this.duration = duration;
        this.elapsed = 0f;
        this.autoReset = false;
        this.ready = false;
    }

    /**
     * Advances the timer by the given delta time.
     *
     * @param dt seconds since the last frame.
     */
    public void update(float dt) {
        if (ready && !autoReset) {
            return;
        }
        elapsed += dt;
        if (elapsed >= duration) {
            ready = true;
            if (autoReset) {
                elapsed -= duration;
            }
        }
    }

    /**
     * Returns true when the timer has elapsed. When auto-reset is enabled,
     * this returns true for exactly one check per cycle; calling this method
     * clears the ready flag so subsequent calls return false until the next
     * elapse.
     *
     * @return true if the timer has elapsed since the last check.
     */
    public boolean isReady() {
        if (ready) {
            if (autoReset) {
                ready = false;
            }
            return true;
        }
        return false;
    }

    /** Resets the timer to zero elapsed time. */
    public void reset() {
        elapsed = 0f;
        ready = false;
    }

    /** @return seconds elapsed since the last reset. */
    public float getElapsed() {
        return elapsed;
    }

    /** @return seconds remaining until the timer elapses. */
    public float getRemaining() {
        float remaining = duration - elapsed;
        return remaining > 0f ? remaining : 0f;
    }

    /** @return progress from 0.0 (just started) to 1.0 (elapsed). */
    public float getProgress() {
        float progress = elapsed / duration;
        return progress > 1f ? 1f : progress;
    }

    /**
     * @param autoReset true to automatically reset when the timer elapses
     *                  (useful for repeating cooldowns).
     */
    public void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }

    /** @return true if the timer auto-resets on elapse. */
    public boolean isAutoReset() {
        return autoReset;
    }
}
