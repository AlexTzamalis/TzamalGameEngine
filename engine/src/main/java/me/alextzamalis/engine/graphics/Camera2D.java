package me.alextzamalis.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;

/**
 * An orthographic 2D camera that produces a combined projection-view matrix
 * for rendering with the {@link BatchRenderer}.
 *
 * <h2>View matrix vs projection matrix</h2>
 * <p>The <strong>projection matrix</strong> defines the visible coordinate
 * space (how world units map to normalised device coordinates). For a 2D
 * game this is typically an orthographic box whose size matches the window
 * or a scaled fraction of it.</p>
 *
 * <p>The <strong>view matrix</strong> represents the camera's own position
 * and orientation in the world. Because OpenGL has no real "camera" concept,
 * the view matrix works by applying the <em>inverse</em> of the camera's
 * transform to every vertex. In other words, the camera "moves the world"
 * in the opposite direction rather than moving itself.</p>
 *
 * <p>When you call {@link #getProjectionViewMatrix()}, this class multiplies
 * the projection by the view matrix so the result can be uploaded directly
 * to the shader as a single uniform.</p>
 *
 * <h2>Dirty-flag caching</h2>
 * <p>Recomputing matrices every frame is wasteful when the camera rarely
 * moves. A dirty flag tracks whether any property (position, zoom,
 * rotation) has changed since the last computation. The combined matrix is
 * only recalculated when it is actually needed and something has changed.</p>
 *
 * @author Alexandros Tzamalis
 * @see BatchRenderer#setProjection(Matrix4f)
 */
public class Camera2D {

    private final Vector2f position;
    private float zoom;
    private float rotation;

    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionViewMatrix;

    private int projectionWidth;
    private int projectionHeight;
    private boolean dirty;

    /**
     * Creates a camera centred at the origin with default zoom (1.0) and
     * no rotation.
     *
     * @param width  initial viewport width in pixels (used for the
     *               orthographic projection).
     * @param height initial viewport height in pixels.
     */
    public Camera2D(int width, int height) {
        this.position = new Vector2f(0f, 0f);
        this.zoom = 1.0f;
        this.rotation = 0f;

        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.projectionViewMatrix = new Matrix4f();

        this.projectionWidth = width;
        this.projectionHeight = height;
        this.dirty = true;

        recalculateProjection();
    }

    /**
     * Returns the combined projection-view matrix, recomputing it only if
     * position, zoom, or rotation have changed since the last call.
     *
     * @return the projection * view matrix ready to upload to the shader.
     */
    public Matrix4f getProjectionViewMatrix() {
        if (dirty) {
            recalculate();
            dirty = false;
        }
        return projectionViewMatrix;
    }

    /**
     * Updates the orthographic projection to match a new viewport size.
     * Call this when the window is resized.
     *
     * @param width  new viewport width in pixels.
     * @param height new viewport height in pixels.
     */
    public void adjustProjection(int width, int height) {
        this.projectionWidth = width;
        this.projectionHeight = height;
        recalculateProjection();
        dirty = true;
    }

    // Setters (each marks the camera dirty)
    /**
     * @param position the new world-space position the camera centres on.
     */
    public void setPosition(Vector2f position) {
        this.position.set(position);
        dirty = true;
    }

    /**
     * @param zoom the new zoom level. Values greater than 1 zoom in
     *             (showing less of the world), values less than 1 zoom out.
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
        dirty = true;
    }

    /**
     * @param rotation camera rotation in degrees (counter-clockwise).
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
        dirty = true;
    }

    // Getters

    /** @return the current camera position (reference, not a copy). */
    public Vector2f getPosition() {
        return position;
    }

    /** @return the current zoom level. */
    public float getZoom() {
        return zoom;
    }

    /** @return the current rotation in degrees. */
    public float getRotation() {
        return rotation;
    }

    // Internal computation
    private void recalculateProjection() {
        float halfW = projectionWidth / 2.0f;
        float halfH = projectionHeight / 2.0f;
        projectionMatrix.identity().ortho(-halfW, halfW, -halfH, halfH, -1f, 1f);
    }

    private void recalculate() {
        recalculateProjection();

        viewMatrix.identity();

        // Zoom scales the view (applied first so rotation and translation
        // operate in zoomed space)
        viewMatrix.scale(zoom, zoom, 1f);

        // Rotate around the Z axis
        if (rotation != 0f) {
            viewMatrix.rotateZ((float) Math.toRadians(-rotation));
        }

        // Translate the world in the opposite direction of the camera position
        viewMatrix.translate(-position.x, -position.y, 0f);

        // Combined: projection * view
        projectionMatrix.mul(viewMatrix, projectionViewMatrix);
    }
}
