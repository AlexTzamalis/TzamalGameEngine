package me.alextzamalis.engine.scene;

/**
 * A lightweight game entity that combines a {@link Transform} and an
 * optional {@link Sprite} for rendering.
 *
 * <p>This is not a full Entity-Component-System; it is a practical,
 * simple game object suitable for small-to-medium 2D games. Subclass
 * and override {@link #update(float)} to add custom behaviour.</p>
 *
 * @author Alexandros Tzamalis
 * @see Transform
 * @see Sprite
 * @see Scene
 */
public class GameObject {

    private final String name;
    private final Transform transform;
    private Sprite sprite;
    private int zIndex;

    /**
     * Creates a game object with the given name and transform.
     *
     * @param name a readable identifier (useful for debugging).
     * @param transform the object's position, rotation, and scale.
     */
    public GameObject(String name, Transform transform) {
        this.name = name;
        this.transform = transform;
        this.sprite = null;
        this.zIndex = 0;
    }

    /**
     * Called once per frame by the {@link Scene}. Override in subclasses
     * to add custom logic.
     *
     * @param dt seconds since the last frame.
     */
    public void update(float dt) {
        // Default: no behaviour. Subclasses override as needed.
    }

    /**
     * Attaches a sprite to this game object so it can be rendered.
     *
     * @param sprite the visual component to attach.
     * @return this instance for method chaining.
     */
    public GameObject addSprite(Sprite sprite) {
        this.sprite = sprite;
        return this;
    }

    /** @return the readable name of this object. */
    public String getName() {
        return name;
    }

    /** @return the transform (position, rotation, scale). */
    public Transform getTransform() {
        return transform;
    }

    /** @return the attached sprite, or null if none. */
    public Sprite getSprite() {
        return sprite;
    }

    /** @return the draw order index (lower values are drawn first). */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * @param zIndex draw order index. Lower values are drawn behind
     *               higher values.
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
}
