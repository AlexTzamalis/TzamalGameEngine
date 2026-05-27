package me.alextzamalis.engine.graphics;

import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Plays a one-shot frame sequence and removes itself when finished.
 */
public class OneShotAnimation {

    private final List<Sprite> frames = new ArrayList<>();
    private final float frameDuration;
    private float timer;
    private int frameIndex;
    private boolean finished;

    private float x;
    private float y;
    private float width;
    private float height;
    private float rotationDeg;
    private final Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);

    public OneShotAnimation(float frameDuration) {
        this.frameDuration = frameDuration;
    }

    public void addFrame(Texture texture) {
        frames.add(new Sprite(texture));
    }

    public void setPosition(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRotation(float rotationDeg) {
        this.rotationDeg = rotationDeg;
    }

    public Vector4f getTint() {
        return tint;
    }

    public boolean isFinished() {
        return finished;
    }

    public void update(float dt) {
        if (finished || frames.isEmpty()) {
            finished = true;
            return;
        }
        timer += dt;
        while (timer >= frameDuration) {
            timer -= frameDuration;
            frameIndex++;
            if (frameIndex >= frames.size()) {
                finished = true;
                return;
            }
        }
    }

    public void render(BatchRenderer batch) {
        if (finished || frames.isEmpty()) {
            return;
        }
        Sprite frame = frames.get(frameIndex);
        if (frame.texture == null) {
            return;
        }
        if (rotationDeg != 0f) {
            batch.drawQuad(x, y, width, height, rotationDeg, frame.texture, tint,
                    frame.uvMin, frame.uvMax);
        } else {
            batch.drawQuad(x, y, width, height, frame.texture, tint,
                    frame.uvMin, frame.uvMax);
        }
    }

    public static void updateAll(List<OneShotAnimation> animations, float dt) {
        Iterator<OneShotAnimation> it = animations.iterator();
        while (it.hasNext()) {
            OneShotAnimation anim = it.next();
            anim.update(dt);
            if (anim.isFinished()) {
                it.remove();
            }
        }
    }

    public static void renderAll(BatchRenderer batch, List<OneShotAnimation> animations) {
        for (OneShotAnimation anim : animations) {
            anim.render(batch);
        }
    }
}
