package me.alextzamalis.engine.audio;

import me.alextzamalis.engine.core.Logger;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * OpenAL audio manager for one-shot SFX and looping music.
 *
 * <p>Call {@link #init()} during engine startup and {@link #dispose()}
 * during shutdown. Audio files are loaded from classpath resources via
 * {@link #loadSound(String)} and {@link #loadMusic(String)}.</p>
 */
public final class AudioManager {

    private static final Map<String, Sound> sounds = new HashMap<>();
    private static final Map<String, MusicTrack> musicTracks = new HashMap<>();
    private static final Map<String, Integer> buffers = new HashMap<>();
    private static final List<Integer> ephemeralSources = new ArrayList<>();

    private static long device;
    private static long context;
    private static boolean enabled;
    private static float masterVolume = 1f;
    private static float musicVolume = 1f;
    private static float sfxVolume = 1f;
    private static int activeMusicSource;

    private AudioManager() {
    }

    public static void init() {
        if (enabled) {
            return;
        }

        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0L) {
            Logger.warn("Audio", "OpenAL device unavailable. Audio disabled.");
            return;
        }

        context = alcCreateContext(device, (IntBuffer) null);
        if (context == 0L) {
            alcCloseDevice(device);
            device = 0L;
            Logger.warn("Audio", "OpenAL context unavailable. Audio disabled.");
            return;
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
        enabled = true;
        alListener3f(AL_POSITION, 0f, 0f, 0f);
        alListener3f(AL_VELOCITY, 0f, 0f, 0f);
        alListenerf(AL_GAIN, masterVolume);
        Logger.info("Audio", "OpenAL initialised.");
    }

    public static void dispose() {
        for (MusicTrack track : musicTracks.values()) {
            track.dispose();
        }
        musicTracks.clear();

        for (int source : ephemeralSources) {
            alDeleteSources(source);
        }
        ephemeralSources.clear();

        for (int buffer : buffers.values()) {
            alDeleteBuffers(buffer);
        }
        buffers.clear();
        sounds.clear();

        if (context != 0L) {
            alcMakeContextCurrent(0L);
            alcDestroyContext(context);
            context = 0L;
        }
        if (device != 0L) {
            alcCloseDevice(device);
            device = 0L;
        }

        enabled = false;
        activeMusicSource = 0;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static float getSfxVolume() {
        return sfxVolume;
    }

    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
        if (!enabled) {
            return;
        }
        alListenerf(AL_GAIN, masterVolume);
        applyMusicGain();
    }

    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        applyMusicGain();
    }

    public static void setSfxVolume(float volume) {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public static void applyMusicGain() {
        if (!enabled || activeMusicSource == 0) {
            return;
        }
        alSourcef(activeMusicSource, AL_GAIN, masterVolume * musicVolume);
    }

    public static float getEffectiveMusicGain(float volumeScale) {
        return masterVolume * musicVolume * volumeScale;
    }

    public static float getEffectiveSfxGain(float volumeScale) {
        return masterVolume * sfxVolume * volumeScale;
    }

    public static Sound loadSound(String resourcePath) {
        Sound cached = sounds.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        int bufferId = loadBuffer(resourcePath);
        if (bufferId == 0) {
            return null;
        }
        Sound sound = new Sound(bufferId);
        sounds.put(resourcePath, sound);
        return sound;
    }

    public static MusicTrack loadMusic(String resourcePath) {
        MusicTrack cached = musicTracks.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        int bufferId = loadBuffer(resourcePath);
        if (bufferId == 0) {
            return null;
        }
        int sourceId = alGenSources();
        MusicTrack track = new MusicTrack(bufferId, sourceId);
        musicTracks.put(resourcePath, track);
        return track;
    }

    public static void stopMusic() {
        if (activeMusicSource != 0) {
            alSourceStop(activeMusicSource);
            activeMusicSource = 0;
        }
    }

    public static void update() {
        if (!enabled) {
            return;
        }
        Iterator<Integer> it = ephemeralSources.iterator();
        while (it.hasNext()) {
            int source = it.next();
            if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
                alDeleteSources(source);
                it.remove();
            }
        }
    }

    static void playBuffer(int bufferId, float volumeScale, boolean looping) {
        if (!enabled || bufferId == 0) {
            return;
        }
        int source = alGenSources();
        alSourcei(source, AL_BUFFER, bufferId);
        alSourcei(source, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
        alSourcef(source, AL_GAIN, getEffectiveSfxGain(volumeScale));
        alSourcePlay(source);
        if (!looping) {
            ephemeralSources.add(source);
        }
    }

    static int getActiveMusicSource() {
        return activeMusicSource;
    }

    static void setActiveMusicSource(int sourceId) {
        activeMusicSource = sourceId;
    }

    private static int loadBuffer(String resourcePath) {
        Integer cached = buffers.get(resourcePath);
        if (cached != null) {
            return cached;
        }

        PcmData pcm = AudioLoader.loadResource(resourcePath);
        if (pcm == null) {
            return 0;
        }

        int format = pcm.channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        int bufferId = alGenBuffers();
        alBufferData(bufferId, format, pcm.samples, pcm.sampleRate);
        buffers.put(resourcePath, bufferId);
        Logger.info("Audio", "Loaded " + resourcePath);
        return bufferId;
    }
}
