package me.alextzamalis.engine.audio;

import static org.lwjgl.openal.AL10.*;

/**
 * One-shot sound effect backed by a single OpenAL buffer.
 */
public class Sound {

    private final int bufferId;

    Sound(int bufferId) {
        this.bufferId = bufferId;
    }

    public void play() {
        play(1f);
    }

    public void play(float volumeScale) {
        AudioManager.playBuffer(bufferId, volumeScale, false);
    }

    int getBufferId() {
        return bufferId;
    }
}
