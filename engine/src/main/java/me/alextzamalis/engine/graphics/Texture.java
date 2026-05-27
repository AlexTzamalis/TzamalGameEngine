package me.alextzamalis.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.stb.STBImage.*;

/**
 * Loads an image file into an OpenGL texture and manages its lifetime.
 *
 * <h2>Texture slots</h2>
 * <p>OpenGL supports multiple texture units (GL_TEXTURE0 through
 * GL_TEXTURE15 on most hardware). Before sampling a texture in a
 * shader, the application must bind it to a specific slot with
 * {@link #bind(int)}. The corresponding sampler uniform in the
 * shader should be set to the same slot index.</p>
 *
 * <h2>Filtering</h2>
 * <p>This class defaults to {@code GL_NEAREST} for both min and mag
 * filters, which gives pixel-art style rendering (no blurring).
 * Change these to {@code GL_LINEAR} if you need smooth scaling.</p>
 *
 * <h2>Memory lifecycle</h2>
 * <p>STB allocates native memory for the decoded pixel data. That
 * memory is freed with {@code stbi_image_free} immediately after
 * the pixels are uploaded to the GPU via {@code glTexImage2D}.
 * The GPU-side texture is freed when {@link #dispose()} is called.</p>
 *
 * @author Alexandros Tzamalis
 * @see Shader
 * @see BatchRenderer
 */
public class Texture {

    private final int textureId;
    private final int width;
    private final int height;

    /**
     * Wraps an already-created OpenGL texture ID. Used by engine
     * subsystems (like the font rasterizer) that create textures
     * directly via GL calls.
     *
     * <p>The caller is responsible for the GL texture's configuration
     * (filtering, wrapping, format). This constructor simply records
     * the ID so the texture can participate in batch rendering and
     * be disposed through the normal lifecycle.</p>
     *
     * @param textureId the existing OpenGL texture object ID.
     * @param width     texture width in pixels.
     * @param height    texture height in pixels.
     */
    public Texture(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }

    /**
     * Loads an image from disk and uploads it as an OpenGL texture.
     *
     * <p>Supported formats include PNG, JPG, BMP, TGA, and any other
     * format that STB's {@code stbi_load} handles.</p>
     *
     * @param filePath absolute or relative path to the image file.
     * @throws RuntimeException if the file cannot be loaded.
     */
    public Texture(String filePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wBuf = stack.mallocInt(1);
            IntBuffer hBuf = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixels = stbi_load(filePath, wBuf, hBuf, channels, 0);
            if (pixels == null) {
                throw new RuntimeException(
                        "Failed to load texture: " + filePath
                        + " -- " + stbi_failure_reason());
            }

            this.width = wBuf.get(0);
            this.height = hBuf.get(0);
            int numChannels = channels.get(0);

            int internalFormat;
            int format;
            if (numChannels == 4) {
                internalFormat = GL_RGBA;
                format = GL_RGBA;
            } else if (numChannels == 3) {
                internalFormat = GL_RGB;
                format = GL_RGB;
            } else {
                stbi_image_free(pixels);
                throw new RuntimeException(
                        "Unsupported channel count (" + numChannels
                        + ") in texture: " + filePath);
            }

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, internalFormat,
                    width, height, 0, format, GL_UNSIGNED_BYTE, pixels);

            stbi_image_free(pixels);
        }
    }

    /**
     * Loads an image from a classpath resource and uploads it as an OpenGL texture.
     *
     * <p>This works identically to the file-path constructor but reads bytes
     * from the classpath via {@link Class#getResourceAsStream}. Resource
     * paths should start with {@code /} for absolute classpath resolution
     * (e.g. {@code "/atlas/img.png"}).</p>
     *
     * @param resourcePath classpath path to the image resource.
     * @return a ready-to-use Texture instance.
     * @throws RuntimeException if the resource cannot be found or decoded.
     */
    public static Texture fromResource(String resourcePath) {
        byte[] data;
        try (InputStream is = Texture.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Texture resource not found: " + resourcePath);
            }
            data = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture resource: " + resourcePath, e);
        }

        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        buf.put(data).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wBuf = stack.mallocInt(1);
            IntBuffer hBuf = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixels = stbi_load_from_memory(buf, wBuf, hBuf, channels, 0);
            if (pixels == null) {
                MemoryUtil.memFree(buf);
                throw new RuntimeException(
                        "Failed to decode texture resource: " + resourcePath
                        + " -- " + stbi_failure_reason());
            }

            int width = wBuf.get(0);
            int height = hBuf.get(0);
            int numChannels = channels.get(0);

            int internalFormat;
            int format;
            if (numChannels == 4) {
                internalFormat = GL_RGBA;
                format = GL_RGBA;
            } else if (numChannels == 3) {
                internalFormat = GL_RGB;
                format = GL_RGB;
            } else {
                stbi_image_free(pixels);
                MemoryUtil.memFree(buf);
                throw new RuntimeException(
                        "Unsupported channel count (" + numChannels
                        + ") in texture resource: " + resourcePath);
            }

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, internalFormat,
                    width, height, 0, format, GL_UNSIGNED_BYTE, pixels);

            stbi_image_free(pixels);
            MemoryUtil.memFree(buf);

            return new Texture(texId, width, height);
        }
    }

    /**
     * Binds this texture to the given texture unit slot.
     *
     * @param slot the slot index (0 for GL_TEXTURE0, 1 for GL_TEXTURE1, etc.).
     */
    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Unbinds any 2D texture from the currently active texture unit.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Deletes the OpenGL texture, freeing GPU memory.
     */
    public void dispose() {
        glDeleteTextures(textureId);
    }

    /** @return the OpenGL texture object ID. */
    public int getTextureId() {
        return textureId;
    }

    /** @return image width in pixels. */
    public int getWidth() {
        return width;
    }

    /** @return image height in pixels. */
    public int getHeight() {
        return height;
    }
}
