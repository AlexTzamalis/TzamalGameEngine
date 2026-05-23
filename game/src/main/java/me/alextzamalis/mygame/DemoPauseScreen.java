package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.Shader;
import me.alextzamalis.engine.screen.GameScreen;

import org.joml.Vector4f;

/**
 * A transparent overlay screen that pauses gameplay and renders a
 * dark tint over the play screen with a colored "PAUSED" indicator.
 *
 * @author Alexandros Tzamalis
 */
public class DemoPauseScreen extends GameScreen {

    private Window window;
    private BatchRenderer batchRenderer;
    private Camera2D camera;

    @Override
    public void init(Window window) {
        this.window = window;

        Shader shader = AssetManager.getShader("default");
        batchRenderer = new BatchRenderer(shader);
        camera = new Camera2D(window.getWidth(), window.getHeight());

        System.out.println("[Pause] Pause screen initialised. Press ESC to resume.");
    }

    @Override
    public void update(float dt) {
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            screenManager.popScreen();
        }
    }

    @Override
    public void render() {
        int w = window.getWidth();
        int h = window.getHeight();
        camera.adjustProjection(w, h);

        float halfW = w / 2.0f;
        float halfH = h / 2.0f;

        batchRenderer.setProjection(camera.getProjectionViewMatrix());
        batchRenderer.beginBatch();

        // Dark semi-transparent overlay covering the whole viewport
        batchRenderer.drawQuad(-halfW, -halfH, w, h,
                new Vector4f(0f, 0f, 0f, 0.5f));

        // Centered "PAUSED" indicator block
        float blockW = 120f;
        float blockH = 60f;
        batchRenderer.drawQuad(-blockW / 2f, -blockH / 2f, blockW, blockH,
                new Vector4f(0.9f, 0.3f, 0.3f, 0.9f));

        batchRenderer.endBatch();
        batchRenderer.flush();
    }

    @Override
    public void dispose() {
        if (batchRenderer != null) {
            batchRenderer.dispose();
        }
        System.out.println("[Pause] Pause screen disposed.");
    }

    @Override
    public boolean isTransparent() {
        return true;
    }
}
