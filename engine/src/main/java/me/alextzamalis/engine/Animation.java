package me.alextzamalis.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Cycles through a list of {@link Sprite} frames at a configurable rate.
 *
 * <p>The animation does not modify a {@link GameObject}'s sprite directly.
 * Game code should call {@link #getCurrentFrame()} each frame and apply the
 * returned sprite to the object. This keeps Animation decoupled from the
 * component model.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // Build frames from a sprite sheet
 * List<Sprite> walkFrames = new ArrayList<>();
 * for (int i = 0; i < 4; i++) {
 *     walkFrames.add(sheet.getSprite(i, 0));
 * }
 *
 * Animation walkAnim = new Animation(walkFrames, 0.15f);
 *
 * // In the game update loop:
 * walkAnim.update(dt);
 * player.addSprite(walkAnim.getCurrentFrame());
 * }</pre>
 *
 * @author Alexandros Tzamalis
 * @see Sprite
 * @see SpriteSheet
 */
public class Animation {

    private final List<Sprite> frames;
    private final float frameDuration;
    private float timer;
    private int frameIndex;
    private boolean looping;
    private boolean finished;

    /**
     * Creates an animation from the given frames.
     *
     * @param frames        list of sprite frames (defensive copy is made).
     * @param frameDuration seconds each frame is displayed.
     * @throws IllegalArgumentException if frames is empty or frameDuration
     *                                  is non-positive.
     */
    public Animation(List<Sprite> frames, float frameDuration) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("Animation requires at least one frame");
        }
        if (frameDuration <= 0f) {
            throw new IllegalArgumentException(
                    "Frame duration must be positive: " + frameDuration);
        }
        this.frames = new ArrayList<>(frames);
        this.frameDuration = frameDuration;
        this.timer = 0f;
        this.frameIndex = 0;
        this.looping = true;
        this.finished = false;
    }

    /**
     * Advances the animation timer and updates the current frame index.
     *
     * @param dt seconds since the last frame.
     */
    public void update(float dt) {
        if (finished) {
            return;
        }

        timer += dt;
        while (timer >= frameDuration) {
            timer -= frameDuration;
            frameIndex++;

            if (frameIndex >= frames.size()) {
                if (looping) {
                    frameIndex = 0;
                } else {
                    frameIndex = frames.size() - 1;
                    finished = true;
                    return;
                }
            }
        }
    }

    /** @return the sprite for the current animation frame. */
    public Sprite getCurrentFrame() {
        return frames.get(frameIndex);
    }

    /** Resets the animation to the first frame. */
    public void reset() {
        timer = 0f;
        frameIndex = 0;
        finished = false;
    }

    /**
     * @param looping true to loop the animation, false to stop at the
     *                last frame.
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    /** @return true if the animation loops. */
    public boolean isLooping() {
        return looping;
    }

    /**
     * @return true if the animation is non-looping and has reached
     *         the last frame.
     */
    public boolean isFinished() {
        return finished;
    }

    /** @return the index of the current frame. */
    public int getFrameIndex() {
        return frameIndex;
    }
}
