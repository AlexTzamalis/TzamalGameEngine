package me.alextzamalis.engine.audio;

import static org.lwjgl.openal.AL10.*;

/**
 * Looping music track backed by a dedicated OpenAL source.
 */
public class MusicTrack {

    private final int bufferId;
    private final int sourceId;

    MusicTrack(int bufferId, int sourceId) {
        this.bufferId = bufferId;
        this.sourceId = sourceId;
    }

    public void playLoop() {
        playLoop(1f);
    }

    public void playLoop(float volumeScale) {
        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcei(sourceId, AL_LOOPING, AL_TRUE);
        alSourcef(sourceId, AL_GAIN, AudioManager.getEffectiveMusicGain(volumeScale));
        alSourcePlay(sourceId);
        AudioManager.setActiveMusicSource(sourceId);
    }

    public void stop() {
        alSourceStop(sourceId);
        if (AudioManager.getActiveMusicSource() == sourceId) {
            AudioManager.setActiveMusicSource(0);
        }
    }

    public void pause() {
        alSourcePause(sourceId);
    }

    public void resume() {
        alSourcePlay(sourceId);
    }

    public int getSourceId() {
        return sourceId;
    }

    void dispose() {
        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
        }
    }
}
