package me.alextzamalis.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility for querying overlap between {@link GameObject}s
 * using their {@link Transform} data.
 *
 * <p>Bounding boxes are computed on the fly from each object's
 * transform (position = bottom-left, scale = size). No persistent
 * spatial data structures are maintained.</p>
 *
 * <p>Typical usage in a game update loop:</p>
 * <pre>{@code
 * List<GameObject> hits = CollisionSystem.checkCollisions(player, enemies);
 * for (GameObject enemy : hits) {
 *     // handle collision
 * }
 * }</pre>
 *
 * @author Alexandros Tzamalis
 * @see AABB
 * @see GameObject
 */
public final class CollisionSystem {

    private CollisionSystem() {
        // Utility class, no instances.
    }

    /**
     * Returns all targets whose bounding boxes overlap with the source's
     * bounding box. The source itself is skipped if it appears in the
     * target list.
     *
     * @param source  the game object to test against.
     * @param targets the list of potential collision targets.
     * @return a list of targets that overlap with source (may be empty).
     */
    public static List<GameObject> checkCollisions(GameObject source,
                                                   List<GameObject> targets) {
        List<GameObject> result = new ArrayList<>();
        AABB sourceBox = new AABB(source.getTransform());

        for (int i = 0; i < targets.size(); i++) {
            GameObject target = targets.get(i);
            if (target == source) {
                continue;
            }
            AABB targetBox = new AABB(target.getTransform());
            if (sourceBox.intersects(targetBox)) {
                result.add(target);
            }
        }
        return result;
    }

    /**
     * Simple pair check: tests whether two game objects overlap.
     *
     * @param a first game object.
     * @param b second game object.
     * @return true if their bounding boxes overlap.
     */
    public static boolean collides(GameObject a, GameObject b) {
        AABB boxA = new AABB(a.getTransform());
        AABB boxB = new AABB(b.getTransform());
        return boxA.intersects(boxB);
    }
}
