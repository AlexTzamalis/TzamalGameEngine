package me.alextzamalis.engine.graphics;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Horizontal left-to-right wipe between two fullscreen textures.
 */
public class WipeTransition {

    private float progress;
    private float duration = 0.9f;
    private boolean active;

    public void start(float durationSeconds) {
        this.duration = Math.max(0.1f, durationSeconds);
        this.progress = 0f;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        progress += dt / duration;
        if (progress >= 1f) {
            progress = 1f;
            active = false;
        }
    }

    public boolean isActive() {
        return active || progress < 1f;
    }

    public boolean isFinished() {
        return !active && progress >= 1f;
    }

    public float getProgress() {
        return progress;
    }

    public void reset() {
        progress = 0f;
        active = false;
    }

    /**
     * Renders old background full screen, then reveals new background left-to-right.
     *
     * @param x      bottom-left X of the play area.
     * @param y      bottom-left Y of the play area.
     * @param width  play area width.
     * @param height play area height.
     */
    public void render(BatchRenderer batch, Texture oldTexture, Texture newTexture,
                       float x, float y, float width, float height) {
        if (oldTexture == null && newTexture == null) {
            return;
        }

        Vector2f uvFullMin = new Vector2f(0f, 0f);
        Vector2f uvFullMax = new Vector2f(1f, 1f);

        if (oldTexture != null) {
            float oldAlpha = 1f - progress * 0.35f;
            batch.drawQuad(x, y, width, height, oldTexture,
                    new Vector4f(1f, 1f, 1f, oldAlpha), uvFullMin, uvFullMax);
        }

        if (newTexture != null && progress > 0f) {
            float revealW = width * progress;
            Vector2f uvMax = new Vector2f(progress, 1f);
            batch.drawQuad(x, y, revealW, height, newTexture,
                    new Vector4f(1f, 1f, 1f, 1f), uvFullMin, uvMax);
        }
    }
}
