package me.alextzamalis.engine;

import org.joml.Vector2f;

/**
 * A simple 2D transform representing position, rotation, and scale.
 *
 * <p>This is a lightweight data container (component-style), so all
 * fields are public for direct access. It is not an API boundary that
 * needs encapsulation.</p>
 *
 * @author Alexandros Tzamalis
 */
public class Transform {

    /** World-space position. */
    public Vector2f position;

    /** Rotation in degrees (counter-clockwise). */
    public float rotation;

    /** Scale factor per axis. Default is (1, 1). */
    public Vector2f scale;

    /** Creates a transform at the origin with no rotation and unit scale. */
    public Transform() {
        this.position = new Vector2f(0f, 0f);
        this.rotation = 0f;
        this.scale = new Vector2f(1f, 1f);
    }

    /**
     * Creates a transform at the given position with no rotation and
     * unit scale.
     *
     * @param position world-space position.
     */
    public Transform(Vector2f position) {
        this.position = new Vector2f(position);
        this.rotation = 0f;
        this.scale = new Vector2f(1f, 1f);
    }

    /**
     * Creates a transform at the given position with the given scale,
     * and no rotation.
     *
     * @param position world-space position.
     * @param scale    per-axis scale.
     */
    public Transform(Vector2f position, Vector2f scale) {
        this.position = new Vector2f(position);
        this.rotation = 0f;
        this.scale = new Vector2f(scale);
    }

    /**
     * Copy constructor.
     *
     * @param other the transform to copy from.
     */
    public Transform(Transform other) {
        this.position = new Vector2f(other.position);
        this.rotation = other.rotation;
        this.scale = new Vector2f(other.scale);
    }
}
