package me.alextzamalis.engine.scene;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages a collection of {@link GameObject}s and orchestrates the
 * update/render pipeline each frame.
 *
 * <p>The scene handles draw ordering (via {@link GameObject#getZIndex()})
 * and drives the full batch-render sequence: set projection from camera,
 * begin batch, draw all visible objects, end batch, flush.</p>
 *
 * @author Alexandros Tzamalis
 * @see GameObject
 * @see Camera2D
 * @see BatchRenderer
 */
public class Scene {

    private final List<GameObject> gameObjects;
    private boolean renderOrderDirty;

    public Scene() {
        this.gameObjects = new ArrayList<>();
        this.renderOrderDirty = false;
    }

    /**
     * Adds a game object to this scene.
     *
     * @param go the object to add.
     */
    public void addGameObject(GameObject go) {
        gameObjects.add(go);
        renderOrderDirty = true;
    }

    /**
     * Removes a game object from this scene.
     *
     * @param go the object to remove.
     */
    public void removeGameObject(GameObject go) {
        gameObjects.remove(go);
    }

    /**
     * Returns an unmodifiable view of the game objects in this scene.
     * Used by the editor to inspect and display the scene hierarchy.
     *
     * @return read-only list of game objects.
     */
    public List<GameObject> getGameObjects() {
        return Collections.unmodifiableList(gameObjects);
    }

    /**
     * Updates all game objects in the scene.
     *
     * @param dt seconds since the last frame.
     */
    public void update(float dt) {
        for (int i = 0; i < gameObjects.size(); i++) {
            gameObjects.get(i).update(dt);
        }
    }

    /**
     * Renders all game objects that have a sprite attached, sorted by
     * z-index (lowest first). Manages the full batch pipeline.
     *
     * @param batch  the batch renderer to draw with.
     * @param camera the camera providing the projection-view matrix.
     */
    public void render(BatchRenderer batch, Camera2D camera) {
        if (renderOrderDirty) {
            gameObjects.sort(Comparator.comparingInt(GameObject::getZIndex));
            renderOrderDirty = false;
        }

        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            Sprite sprite = go.getSprite();
            if (sprite == null) {
                continue;
            }

            Transform t = go.getTransform();
            float x = t.position.x;
            float y = t.position.y;
            float w = t.scale.x;
            float h = t.scale.y;

            if (sprite.texture != null) {
                batch.drawQuad(x, y, w, h, sprite.texture, sprite.color,
                        sprite.uvMin, sprite.uvMax);
            } else {
                batch.drawQuad(x, y, w, h, sprite.color);
            }
        }

        batch.endBatch();
        batch.flush();
    }

    /**
     * Disposes scene resources. Currently a no-op since individual
     * textures/shaders are managed by the AssetManager.
     */
    public void dispose() {
        gameObjects.clear();
    }
}
