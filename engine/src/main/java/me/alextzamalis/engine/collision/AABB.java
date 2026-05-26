package me.alextzamalis.engine.collision;

import me.alextzamalis.engine.scene.Transform;

/**
 * A simple axis-aligned bounding box for 2D overlap detection.
 *
 * <p>The box is defined by its bottom-left corner (x, y) and its
 * dimensions (width, height). This class does not implement full
 * physics; it only provides overlap and containment queries.</p>
 *
 * @author Alexandros Tzamalis
 * @see CollisionSystem
 * @see Transform
 */
public class AABB {

    public float x;
    public float y;
    public float width;
    public float height;

    /**
     * Creates a bounding box from explicit coordinates and size.
     *
     * @param x      left edge.
     * @param y      bottom edge.
     * @param width  horizontal extent.
     * @param height vertical extent.
     */
    public AABB(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a bounding box from a transform. The position is treated as
     * the bottom-left corner and the scale as the width/height.
     *
     * @param transform source transform.
     */
    public AABB(Transform transform) {
        this.x = transform.position.x;
        this.y = transform.position.y;
        this.width = transform.scale.x;
        this.height = transform.scale.y;
    }

    /**
     * Tests whether this box overlaps with another box.
     *
     * @param other the other bounding box.
     * @return true if the two boxes overlap.
     */
    public boolean intersects(AABB other) {
        return this.x < other.x + other.width
                && this.x + this.width > other.x
                && this.y < other.y + other.height
                && this.y + this.height > other.y;
    }

    /**
     * Tests whether a point is inside this box.
     *
     * @param px point x coordinate.
     * @param py point y coordinate.
     * @return true if the point lies within the box (inclusive on min edges).
     */
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width
                && py >= y && py <= y + height;
    }

    /**
     * Refreshes this box's position and size from a transform.
     *
     * @param t the source transform.
     */
    public void updateFromTransform(Transform t) {
        this.x = t.position.x;
        this.y = t.position.y;
        this.width = t.scale.x;
        this.height = t.scale.y;
    }

    /**
     * Static convenience for a simple overlap check between two boxes.
     *
     * @param a first bounding box.
     * @param b second bounding box.
     * @return true if the two boxes overlap.
     */
    public static boolean checkCollision(AABB a, AABB b) {
        return a.intersects(b);
    }
}
