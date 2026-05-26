package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.Shader;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.screen.GameScreen;

import org.joml.Vector4f;

/**
 * A transparent overlay screen that pauses gameplay and renders a
 * dark tint with "PAUSED" text over the play screen.
 *
 * @author Alexandros Tzamalis
 */
public class DemoPauseScreen extends GameScreen {

    private Window window;
    private BatchRenderer batchRenderer;
    private Camera2D camera;
    private Font font;
    private Font smallFont;

    @Override
    public void init(Window window) {
        this.window = window;

        Shader shader = AssetManager.getShader("default");
        batchRenderer = new BatchRenderer(shader);
        camera = new Camera2D(window.getWidth(), window.getHeight());

        font = AssetManager.loadFontResource("/fonts/default.ttf", 32f);
        smallFont = AssetManager.loadFontResource("/fonts/default.ttf", 16f);

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

        // "PAUSED" title centered on screen
        TextRenderer.drawTextFullyCentered(batchRenderer, font, "PAUSED",
                0f, 20f, new Vector4f(0.9f, 0.3f, 0.3f, 1f));

        // Subtitle below
        TextRenderer.drawTextFullyCentered(batchRenderer, smallFont, "Press ESC to resume",
                0f, -30f, new Vector4f(0.8f, 0.8f, 0.8f, 0.9f));

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
