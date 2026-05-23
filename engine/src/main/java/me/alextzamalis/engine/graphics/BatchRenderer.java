package me.alextzamalis.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * A high-performance 2D quad batch renderer.
 *
 * <h2>Why batching?</h2>
 * <p>Every OpenGL draw call has overhead: the driver must validate
 * state, flush command queues, and potentially stall the pipeline.
 * A naive "one draw call per sprite" approach quickly becomes
 * CPU-bound at a few hundred sprites. Batching solves this by
 * accumulating many quads into a single vertex buffer and issuing
 * one {@code glDrawElements} call per batch.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Setup (once)
 * Shader shader = AssetManager.getShader("default");
 * BatchRenderer batch = new BatchRenderer(shader);
 *
 * // Each frame
 * batch.beginBatch();
 * batch.drawQuad(x, y, w, h, color);          // colored quad
 * batch.drawQuad(x, y, w, h, texture, tint);  // textured quad
 * // ... more quads ...
 * batch.endBatch();
 * batch.flush();
 * }</pre>
 *
 * <h2>Auto-flush</h2>
 * <p>If the CPU-side buffer is full (max quads reached) or all 16
 * texture slots are occupied, {@code drawQuad} will automatically
 * call {@code endBatch() + flush() + beginBatch()} so the caller
 * does not need to track capacity.</p>
 *
 * <h2>Vertex layout</h2>
 * <p>Each vertex has 9 floats: position (2), color (4),
 * texCoords (2), texId (1). Four vertices per quad, six indices
 * per quad (two triangles: 0,1,2, 0,2,3).</p>
 *
 * @author Alexandros Tzamalis
 * @see Shader
 * @see Texture
 */
public class BatchRenderer {

    // Vertex layout: pos(2) + color(4) + uv(2) + texId(1) = 9
    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 4;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;
    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE + TEX_ID_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int MAX_TEXTURE_SLOTS = 16;

    private final int maxQuads;
    private final int maxVertices;
    private final int maxIndices;

    private final float[] vertices;
    private int vertexCount;

    private final Texture[] textureSlots = new Texture[MAX_TEXTURE_SLOTS];
    private int textureSlotIndex;

    private final Shader shader;
    private final int vaoId;
    private final int vboId;
    private final int eboId;

    /**
     * Creates a batch renderer with the default capacity of 1000 quads.
     *
     * @param shader the shader program to use for rendering. The batch
     *               renderer does not own this shader; disposal is the
     *               caller's (or AssetManager's) responsibility.
     */
    public BatchRenderer(Shader shader) {
        this(shader, 1000);
    }

    /**
     * Creates a batch renderer with a custom quad capacity.
     *
     * @param shader   the shader program to use for rendering.
     * @param maxQuads maximum number of quads per batch before an
     *                 automatic flush is triggered.
     */
    public BatchRenderer(Shader shader, int maxQuads) {
        this.shader = shader;
        this.maxQuads = maxQuads;
        this.maxVertices = maxQuads * VERTICES_PER_QUAD;
        this.maxIndices = maxQuads * INDICES_PER_QUAD;
        this.vertices = new float[maxVertices * VERTEX_SIZE];

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Dynamic VBO - allocated once, updated each frame with glBufferSubData
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, (long) maxVertices * VERTEX_SIZE_BYTES, GL_DYNAMIC_DRAW);

        // Static EBO - index pattern is always the same for quads
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, generateIndices(), GL_STATIC_DRAW);

        // Vertex attribute layout
        int offset = 0;

        // Position (location 0)
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, offset);
        glEnableVertexAttribArray(0);
        offset += POS_SIZE * Float.BYTES;

        // Color (location 1)
        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, offset);
        glEnableVertexAttribArray(1);
        offset += COLOR_SIZE * Float.BYTES;

        // Tex coords (location 2)
        glVertexAttribPointer(2, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, offset);
        glEnableVertexAttribArray(2);
        offset += TEX_COORDS_SIZE * Float.BYTES;

        // Tex ID (location 3)
        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, offset);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    /**
     * Resets the batch for a new frame. Call this before any
     * {@code drawQuad} calls.
     */
    public void beginBatch() {
        vertexCount = 0;
        textureSlotIndex = 0;
    }

    /**
     * Uploads accumulated vertex data to the GPU.
     * Call this after all {@code drawQuad} calls for the frame.
     */
    public void endBatch() {
        if (vertexCount == 0) {
            return;
        }
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
    }

    /**
     * Binds the shader, binds all used textures to their slots,
     * issues the draw call, and resets state.
     *
     * <p>Typically called right after {@link #endBatch()}:</p>
     * <pre>{@code
     * batch.endBatch();
     * batch.flush();
     * }</pre>
     */
    public void flush() {
        if (vertexCount == 0) {
            return;
        }

        shader.bind();

        for (int i = 0; i < textureSlotIndex; i++) {
            textureSlots[i].bind(i);
        }

        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);

        int quadCount = vertexCount / VERTICES_PER_QUAD;
        glDrawElements(GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glBindVertexArray(0);

        for (int i = 0; i < textureSlotIndex; i++) {
            textureSlots[i].unbind();
            textureSlots[i] = null;
        }

        shader.unbind();
    }

    /**
     * Uploads the projection matrix to the shader.
     *
     * <p>Call this once per frame (or when the projection changes,
     * e.g. on window resize) before {@link #flush()}.</p>
     *
     * @param projection the orthographic or perspective projection matrix.
     */
    public void setProjection(Matrix4f projection) {
        shader.bind();
        shader.uploadMat4("uProjection", projection);
        shader.unbind();
    }

    // Draw methods

    /**
     * Adds a solid-color quad to the batch.
     *
     * @param x     left edge X position.
     * @param y     bottom edge Y position.
     * @param width quad width.
     * @param height quad height.
     * @param color RGBA color (each component 0.0 to 1.0).
     */
    public void drawQuad(float x, float y, float width, float height, Vector4f color) {
        if (vertexCount + VERTICES_PER_QUAD > maxVertices) {
            autoFlush();
        }

        float texId = -1.0f;

        addVertex(x, y, color, 0f, 0f, texId);
        addVertex(x + width, y, color, 1f, 0f, texId);
        addVertex(x + width, y + height, color, 1f, 1f, texId);
        addVertex(x, y + height, color, 0f, 1f, texId);
    }

    /**
     * Adds a textured quad to the batch with a white tint (no tinting).
     *
     * @param x       left edge X position.
     * @param y       bottom edge Y position.
     * @param width   quad width.
     * @param height  quad height.
     * @param texture the texture to sample.
     */
    public void drawQuad(float x, float y, float width, float height, Texture texture) {
        drawQuad(x, y, width, height, texture, new Vector4f(1f, 1f, 1f, 1f));
    }

    /**
     * Adds a textured quad with a tint color to the batch.
     *
     * <p>The fragment shader multiplies the sampled texture color by
     * the tint, so a white tint (1,1,1,1) leaves the texture
     * unmodified.</p>
     *
     * @param x       left edge X position.
     * @param y       bottom edge Y position.
     * @param width   quad width.
     * @param height  quad height.
     * @param texture the texture to sample.
     * @param tint    RGBA tint color.
     */
    public void drawQuad(float x, float y, float width, float height,
                         Texture texture, Vector4f tint) {
        if (vertexCount + VERTICES_PER_QUAD > maxVertices) {
            autoFlush();
        }

        float texId = getTextureSlot(texture);
        if (texId == -1.0f) {
            autoFlush();
            texId = getTextureSlot(texture);
        }

        addVertex(x, y, tint, 0f, 0f, texId);
        addVertex(x + width, y, tint, 1f, 0f, texId);
        addVertex(x + width, y + height, tint, 1f, 1f, texId);
        addVertex(x, y + height, tint, 0f, 1f, texId);
    }

    /**
     * Adds a textured quad with custom UV coordinates and tint.
     *
     * @param x       left edge X position.
     * @param y       bottom edge Y position.
     * @param width   quad width.
     * @param height  quad height.
     * @param texture the texture to sample.
     * @param tint    RGBA tint color.
     * @param uvMin   bottom-left UV coordinate.
     * @param uvMax   top-right UV coordinate.
     */
    public void drawQuad(float x, float y, float width, float height,
                         Texture texture, Vector4f tint,
                         Vector2f uvMin, Vector2f uvMax) {
        if (vertexCount + VERTICES_PER_QUAD > maxVertices) {
            autoFlush();
        }

        float texId = getTextureSlot(texture);
        if (texId == -1.0f) {
            autoFlush();
            texId = getTextureSlot(texture);
        }

        addVertex(x, y, tint, uvMin.x, uvMin.y, texId);
        addVertex(x + width, y, tint, uvMax.x, uvMin.y, texId);
        addVertex(x + width, y + height, tint, uvMax.x, uvMax.y, texId);
        addVertex(x, y + height, tint, uvMin.x, uvMax.y, texId);
    }

    /**
     * Deletes the VAO, VBO, and EBO.
     *
     * <p>Does not delete the shader, which is owned externally
     * (typically by the AssetManager).</p>
     */
    public void dispose() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
    }

    // Internal helpers

    private void addVertex(float x, float y, Vector4f color,
                           float u, float v, float texId) {
        int base = vertexCount * VERTEX_SIZE;
        vertices[base]     = x;
        vertices[base + 1] = y;
        vertices[base + 2] = color.x;
        vertices[base + 3] = color.y;
        vertices[base + 4] = color.z;
        vertices[base + 5] = color.w;
        vertices[base + 6] = u;
        vertices[base + 7] = v;
        vertices[base + 8] = texId;
        vertexCount++;
    }

    private float getTextureSlot(Texture texture) {
        for (int i = 0; i < textureSlotIndex; i++) {
            if (textureSlots[i].getTextureId() == texture.getTextureId()) {
                return (float) i;
            }
        }
        if (textureSlotIndex >= MAX_TEXTURE_SLOTS) {
            return -1.0f;
        }
        textureSlots[textureSlotIndex] = texture;
        return (float) textureSlotIndex++;
    }

    private void autoFlush() {
        endBatch();
        flush();
        beginBatch();
    }

    private int[] generateIndices() {
        int[] indices = new int[maxIndices];
        for (int i = 0; i < maxQuads; i++) {
            int offsetIdx = i * INDICES_PER_QUAD;
            int offsetVtx = i * VERTICES_PER_QUAD;
            // Triangle 1: bottom-left, bottom-right, top-right
            indices[offsetIdx]     = offsetVtx;
            indices[offsetIdx + 1] = offsetVtx + 1;
            indices[offsetIdx + 2] = offsetVtx + 2;
            // Triangle 2: bottom-left, top-right, top-left
            indices[offsetIdx + 3] = offsetVtx;
            indices[offsetIdx + 4] = offsetVtx + 2;
            indices[offsetIdx + 5] = offsetVtx + 3;
        }
        return indices;
    }
}
