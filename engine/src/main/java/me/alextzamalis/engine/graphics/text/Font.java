package me.alextzamalis.engine.graphics.text;

import me.alextzamalis.engine.graphics.Texture;
import org.joml.Vector2f;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * Loads a TrueType font and rasterizes its ASCII glyphs into a GPU
 * texture atlas using STB TrueType.
 *
 * <h2>How it works</h2>
 * <p>The constructor reads a {@code .ttf} file, uses STB's font packing
 * API to rasterize printable ASCII characters (32-126) into a single-channel
 * bitmap, uploads that bitmap as an OpenGL texture, and extracts per-glyph
 * metrics into a {@link Glyph} array.</p>
 *
 * <h2>Atlas texture format</h2>
 * <p>The atlas is a single-channel (GL_RED) texture. To make it work with
 * the engine's standard fragment shader (which expects RGBA), a texture
 * swizzle mask is applied: R=1, G=1, B=1, A=red channel. This makes the
 * texture appear as white with alpha coverage from the glyph bitmaps.
 * When the shader multiplies by the vertex color (tint), you get colored
 * text with correct alpha blending.</p>
 *
 * <h2>Coordinate systems</h2>
 * <p>STB uses a Y-down coordinate system (standard for fonts and screen
 * rendering), while the engine uses Y-up (standard for OpenGL). The UV
 * coordinates stored in each {@link Glyph} are already converted to
 * OpenGL's bottom-left origin. The {@link TextRenderer} handles the
 * position conversion when drawing.</p>
 *
 * @author Alexandros Tzamalis
 * @see Glyph
 * @see TextRenderer
 */
public class Font {

    private static final int ATLAS_SIZE = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 95; // ASCII 32-126 inclusive

    private final Texture atlasTexture;
    private final Glyph[] glyphs;
    private final float lineHeight;
    private final float ascent;
    private final float pixelHeight;

    /**
     * Loads a TrueType font from a file on disk and rasterizes it at the
     * given pixel height.
     *
     * @param ttfFilePath path to a {@code .ttf} file.
     * @param pixelHeight desired font size in pixels.
     * @throws RuntimeException if the file cannot be read or the font
     *                          cannot be initialized.
     */
    public Font(String ttfFilePath, float pixelHeight) {
        this.pixelHeight = pixelHeight;
        ByteBuffer fontData = readFileToDirectBuffer(ttfFilePath);
        try {
            this.glyphs = new Glyph[NUM_CHARS];
            float[] metrics = rasterizeAndExtract(fontData, glyphs);
            this.atlasTexture = createAtlasTexture();
            this.lineHeight = metrics[0];
            this.ascent = metrics[1];
        } finally {
            MemoryUtil.memFree(fontData);
        }
    }

    /**
     * Private constructor used by the factory methods. Accepts a pre-read
     * direct ByteBuffer that the caller is responsible for freeing.
     */
    private Font(ByteBuffer fontData, float pixelHeight) {
        this.pixelHeight = pixelHeight;
        this.glyphs = new Glyph[NUM_CHARS];
        float[] metrics = rasterizeAndExtract(fontData, glyphs);
        this.atlasTexture = createAtlasTexture();
        this.lineHeight = metrics[0];
        this.ascent = metrics[1];
    }

    /**
     * Loads a TrueType font from a classpath resource and rasterizes it.
     *
     * <p>Resource paths are resolved via the engine's class loader,
     * so files under {@code engine/src/main/resources/} are accessible
     * with a leading {@code /}.</p>
     *
     * @param resourcePath classpath path (e.g. {@code "/fonts/default.ttf"}).
     * @param pixelHeight  desired font size in pixels.
     * @return a ready-to-use Font instance.
     * @throws RuntimeException if the resource cannot be found or read.
     */
    public static Font fromResource(String resourcePath, float pixelHeight) {
        ByteBuffer fontData = readResourceToDirectBuffer(resourcePath);
        try {
            return new Font(fontData, pixelHeight);
        } finally {
            MemoryUtil.memFree(fontData);
        }
    }

    /**
     * Returns the glyph metrics for a character. Characters outside the
     * printable ASCII range (32-126) fall back to the space glyph.
     *
     * @param c the character to look up.
     * @return the Glyph containing UV coords and metrics.
     */
    public Glyph getGlyph(char c) {
        int index = c - FIRST_CHAR;
        if (index < 0 || index >= NUM_CHARS) {
            return glyphs[0]; // fallback to space
        }
        return glyphs[index];
    }

    /**
     * Measures the width in pixels that a string would occupy when rendered.
     *
     * @param text the string to measure.
     * @return total horizontal extent in pixels.
     */
    public float textWidth(String text) {
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = getGlyph(text.charAt(i));
            width += g.advance;
        }
        return width;
    }

    /** @return the atlas texture used for rendering glyphs. */
    public Texture getAtlasTexture() {
        return atlasTexture;
    }

    /** @return vertical distance between baselines in pixels. */
    public float getLineHeight() {
        return lineHeight;
    }

    /** @return distance from baseline to top of tallest glyph in pixels. */
    public float getAscent() {
        return ascent;
    }

    /** @return the pixel height this font was rasterized at. */
    public float getPixelHeight() {
        return pixelHeight;
    }

    /** Deletes the atlas texture from the GPU. */
    public void dispose() {
        atlasTexture.dispose();
    }

    // -- Rasterization internals --

    // Temporary state shared between rasterizeAndExtract and createAtlasTexture.
    // Stored as fields so the two-step process works without returning complex tuples.
    private ByteBuffer tempBitmap;
    private STBTTPackedchar.Buffer tempPackedChars;

    /**
     * Packs all ASCII glyphs into a bitmap and extracts per-glyph metrics.
     * The bitmap and packed char buffer are stored in temp fields for
     * the subsequent createAtlasTexture call.
     *
     * @return float[2]: [lineHeight, ascent]
     */
    private float[] rasterizeAndExtract(ByteBuffer fontData, Glyph[] glyphsOut) {
        int atlasW = ATLAS_SIZE;
        int atlasH = ATLAS_SIZE;

        tempBitmap = MemoryUtil.memCalloc(atlasW * atlasH);
        tempPackedChars = STBTTPackedchar.calloc(NUM_CHARS);
        STBTTPackContext packContext = STBTTPackContext.calloc();

        try {
            if (!stbtt_PackBegin(packContext, tempBitmap, atlasW, atlasH, 0, 1)) {
                throw new RuntimeException("stbtt_PackBegin failed");
            }
            if (!stbtt_PackFontRange(packContext, fontData, 0, pixelHeight,
                    FIRST_CHAR, tempPackedChars)) {
                throw new RuntimeException(
                        "stbtt_PackFontRange failed - atlas may be too small for this font size");
            }
            stbtt_PackEnd(packContext);
        } finally {
            packContext.free();
        }

        // Extract glyph metrics via GetPackedQuad
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xBuf = stack.floats(0f);
            FloatBuffer yBuf = stack.floats(0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < NUM_CHARS; i++) {
                xBuf.put(0, 0f);
                yBuf.put(0, 0f);
                stbtt_GetPackedQuad(tempPackedChars, atlasW, atlasH,
                        i, xBuf, yBuf, quad, false);

                float w = quad.x1() - quad.x0();
                float h = quad.y1() - quad.y0();

                // STB UVs use top-left origin (t=0 at top).
                // OpenGL textures have V=0 at bottom (since we don't flip the bitmap).
                // Convert: glV = 1.0 - stbT
                Vector2f uvMin = new Vector2f(quad.s0(), 1.0f - quad.t1());
                Vector2f uvMax = new Vector2f(quad.s1(), 1.0f - quad.t0());

                glyphsOut[i] = new Glyph(
                        uvMin, uvMax,
                        w, h,
                        quad.x0(),
                        quad.y0(),
                        xBuf.get(0)
                );
            }
        }

        // Extract font vertical metrics for line height calculation
        float lineHeight;
        float ascentPx;

        STBTTFontinfo fontInfo = STBTTFontinfo.calloc();
        try {
            if (!stbtt_InitFont(fontInfo, fontData)) {
                // Fallback: estimate from pixel height
                lineHeight = pixelHeight * 1.2f;
                ascentPx = pixelHeight * 0.8f;
            } else {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer ascentBuf = stack.mallocInt(1);
                    IntBuffer descentBuf = stack.mallocInt(1);
                    IntBuffer lineGapBuf = stack.mallocInt(1);

                    stbtt_GetFontVMetrics(fontInfo, ascentBuf, descentBuf, lineGapBuf);
                    float scale = stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);

                    ascentPx = ascentBuf.get(0) * scale;
                    float descentPx = descentBuf.get(0) * scale;
                    float lineGapPx = lineGapBuf.get(0) * scale;

                    lineHeight = ascentPx - descentPx + lineGapPx;
                }
            }
        } finally {
            fontInfo.free();
        }

        return new float[]{lineHeight, ascentPx};
    }

    /**
     * Uploads the rasterized bitmap as a GL_RED texture with swizzle
     * and frees the temporary buffers.
     */
    private Texture createAtlasTexture() {
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Swizzle: the atlas is single-channel (red only). Map it so the
        // shader sees (1, 1, 1, coverage) - white with alpha from the bitmap.
        // The tint color (vertex color) then controls the actual text color.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_ONE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_ONE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_ONE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_RED);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, ATLAS_SIZE, ATLAS_SIZE, 0,
                GL_RED, GL_UNSIGNED_BYTE, tempBitmap);

        glBindTexture(GL_TEXTURE_2D, 0);

        // Free temporary rasterization buffers
        MemoryUtil.memFree(tempBitmap);
        tempPackedChars.free();
        tempBitmap = null;
        tempPackedChars = null;

        return new Texture(texId, ATLAS_SIZE, ATLAS_SIZE);
    }

    // -- File I/O helpers --

    private static ByteBuffer readFileToDirectBuffer(String filePath) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read font file: " + filePath, e);
        }
    }

    private static ByteBuffer readResourceToDirectBuffer(String resourcePath) {
        try (InputStream is = Font.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Font resource not found: " + resourcePath);
            }
            byte[] data = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read font resource: " + resourcePath, e);
        }
    }
}
