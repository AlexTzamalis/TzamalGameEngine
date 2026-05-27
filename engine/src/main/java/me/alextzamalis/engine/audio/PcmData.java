package me.alextzamalis.engine.audio;

import java.nio.ShortBuffer;

/**
 * Decoded PCM audio samples ready for OpenAL upload.
 */
public final class PcmData {

    public final ShortBuffer samples;
    public final int channels;
    public final int sampleRate;

    public PcmData(ShortBuffer samples, int channels, int sampleRate) {
        this.samples = samples;
        this.channels = channels;
        this.sampleRate = sampleRate;
    }
}
