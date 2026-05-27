package me.alextzamalis.engine.audio;

import me.alextzamalis.engine.core.Logger;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Decodes PCM audio from classpath WAV or OGG resources.
 */
public final class AudioLoader {

    private static final int WAVE_FORMAT_PCM = 1;
    private static final int WAVE_FORMAT_IEEE_FLOAT = 3;

    private AudioLoader() {
    }

    public static PcmData loadResource(String resourcePath) {
        byte[] bytes = readResourceBytes(resourcePath);
        if (bytes == null) {
            return null;
        }

        String lower = resourcePath.toLowerCase();
        if (lower.endsWith(".ogg")) {
            return decodeOgg(bytes, resourcePath);
        }
        if (lower.endsWith(".wav")) {
            return decodeWav(bytes, resourcePath);
        }

        Logger.warn("Audio", "Unsupported audio format: " + resourcePath);
        return null;
    }

    private static byte[] readResourceBytes(String resourcePath) {
        try (InputStream is = AudioLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                Logger.warn("Audio", "Audio resource not found: " + resourcePath);
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            Logger.error("Audio", "Failed to read audio resource: " + resourcePath);
            return null;
        }
    }

    private static PcmData decodeOgg(byte[] bytes, String resourcePath) {
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        encoded.put(bytes).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(encoded, channels, sampleRate);
            if (pcm == null) {
                Logger.warn("Audio", "Failed to decode OGG: " + resourcePath);
                return null;
            }
            return new PcmData(pcm, channels.get(0), sampleRate.get(0));
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    private static PcmData decodeWav(byte[] bytes, String resourcePath) {
        if (bytes.length < 44) {
            Logger.warn("Audio", "WAV too small: " + resourcePath);
            return null;
        }

        if (bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F') {
            Logger.warn("Audio", "Not a RIFF WAV: " + resourcePath);
            return null;
        }

        FmtChunk fmt = readFmtChunk(bytes);
        if (fmt == null) {
            Logger.warn("Audio", "WAV fmt chunk missing: " + resourcePath);
            return null;
        }

        if (fmt.formatTag != WAVE_FORMAT_PCM && fmt.formatTag != WAVE_FORMAT_IEEE_FLOAT) {
            Logger.warn("Audio", "Unsupported WAV compression (format " + fmt.formatTag + "): " + resourcePath);
            return null;
        }

        int dataOffset = findChunk(bytes, "data");
        if (dataOffset < 0) {
            Logger.warn("Audio", "WAV data chunk missing: " + resourcePath);
            return null;
        }

        int dataSize = readLe32(bytes, dataOffset - 4);
        if (dataOffset + dataSize > bytes.length) {
            dataSize = bytes.length - dataOffset;
        }

        int frameSize = fmt.blockAlign > 0 ? fmt.blockAlign : (fmt.channels * fmt.bitsPerSample / 8);
        if (frameSize <= 0) {
            Logger.warn("Audio", "Invalid WAV frame size: " + resourcePath);
            return null;
        }

        int frameCount = dataSize / frameSize;
        int sampleCount = frameCount * fmt.channels;
        ShortBuffer pcm = MemoryUtil.memAllocShort(sampleCount);

        int pos = dataOffset;
        for (int frame = 0; frame < frameCount; frame++) {
            for (int ch = 0; ch < fmt.channels; ch++) {
                pcm.put(readSampleAsShort(bytes, pos, fmt.bitsPerSample, fmt.formatTag));
                pos += fmt.bitsPerSample / 8;
            }
        }
        pcm.flip();
        return new PcmData(pcm, fmt.channels, fmt.sampleRate);
    }

    private static short readSampleAsShort(byte[] bytes, int offset, int bitsPerSample, int formatTag) {
        if (formatTag == WAVE_FORMAT_IEEE_FLOAT && bitsPerSample == 32) {
            int bits = readLe32(bytes, offset);
            float sample = Float.intBitsToFloat(bits);
            sample = Math.max(-1f, Math.min(1f, sample));
            return (short) Math.round(sample * 32767f);
        }
        if (bitsPerSample == 8) {
            return (short) (((bytes[offset] & 0xFF) - 128) << 8);
        }
        if (bitsPerSample == 16) {
            return (short) ((bytes[offset + 1] << 8) | (bytes[offset] & 0xFF));
        }
        if (bitsPerSample == 24) {
            int raw = (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16);
            if ((raw & 0x800000) != 0) {
                raw |= ~0xFFFFFF;
            }
            return (short) (raw >> 8);
        }
        if (bitsPerSample == 32) {
            int raw = readLe32(bytes, offset);
            return (short) (raw >> 16);
        }
        return 0;
    }

    private static FmtChunk readFmtChunk(byte[] bytes) {
        int pos = 12;
        while (pos + 8 <= bytes.length) {
            String id = chunkId(bytes, pos);
            int chunkSize = readLe32(bytes, pos + 4);
            if ("fmt ".equals(id) && pos + 8 + chunkSize <= bytes.length) {
                int base = pos + 8;
                FmtChunk fmt = new FmtChunk();
                fmt.formatTag = readLe16(bytes, base);
                fmt.channels = readLe16(bytes, base + 2);
                fmt.sampleRate = readLe32(bytes, base + 4);
                fmt.blockAlign = readLe16(bytes, base + 12);
                fmt.bitsPerSample = readLe16(bytes, base + 14);
                return fmt;
            }
            pos += 8 + Math.max(0, chunkSize);
            if (chunkSize % 2 != 0) {
                pos++;
            }
        }
        return null;
    }

    private static String chunkId(byte[] bytes, int offset) {
        return new String(bytes, offset, 4);
    }

    private static int findChunk(byte[] bytes, String id) {
        int pos = 12;
        while (pos + 8 <= bytes.length) {
            if (bytes[pos] == id.charAt(0) && bytes[pos + 1] == id.charAt(1)
                    && bytes[pos + 2] == id.charAt(2) && bytes[pos + 3] == id.charAt(3)) {
                return pos + 8;
            }
            int chunkSize = readLe32(bytes, pos + 4);
            pos += 8 + chunkSize;
            if (chunkSize % 2 != 0) {
                pos++;
            }
        }
        return -1;
    }

    private static int readLe16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static int readLe32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static class FmtChunk {
        int formatTag;
        int channels;
        int sampleRate;
        int blockAlign;
        int bitsPerSample;
    }
}
