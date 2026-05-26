package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.Shader;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.screen.GameScreen;
import me.alextzamalis.engine.ui.UIButton;
import me.alextzamalis.engine.ui.UIManager;

import org.joml.Vector4f;

public class DemoMenuScreen extends GameScreen {

    private Window window;
    private BatchRenderer batchRenderer;
    private Camera2D camera;
    private Font titleFont;
    private Font buttonFont;
    private UIManager uiManager;

    @Override
    public void init(Window window) {
        this.window = window;

        Shader shader = AssetManager.getShader("default");
        if (shader == null) {
            shader = Shader.fromResource("/shaders/default.vert", "/shaders/default.frag");
            AssetManager.addShader("default", shader);
            shader.bind();
            for (int i = 0; i < 16; i++) {
                shader.uploadTexture("uTextures[" + i + "]", i);
            }
            shader.unbind();
        }

        batchRenderer = new BatchRenderer(shader);
        camera = new Camera2D(window.getWidth(), window.getHeight());

        titleFont = AssetManager.loadFontResource("/fonts/default.ttf", 32f);
        buttonFont = AssetManager.loadFontResource("/fonts/default.ttf", 16f);

        uiManager = new UIManager();

        // Play button
        UIButton playBtn = new UIButton(-100f, -10f, 200f, 50f, "PLAY");
        playBtn.getNormalColor().set(0.2f, 0.5f, 0.2f, 0.9f);
        playBtn.getHoverColor().set(0.3f, 0.7f, 0.3f, 0.95f);
        playBtn.getPressColor().set(0.1f, 0.4f, 0.1f, 1.0f);
        playBtn.setOnClick(() -> screenManager.swapScreen(new DemoPlayScreen()));
        uiManager.addElement(playBtn);

        // Quit button
        UIButton quitBtn = new UIButton(-100f, -80f, 200f, 50f, "QUIT");
        quitBtn.getNormalColor().set(0.5f, 0.2f, 0.2f, 0.9f);
        quitBtn.getHoverColor().set(0.7f, 0.3f, 0.3f, 0.95f);
        quitBtn.getPressColor().set(0.4f, 0.1f, 0.1f, 1.0f);
        quitBtn.setOnClick(() -> window.requestClose());
        uiManager.addElement(quitBtn);

        System.out.println("[Menu] Main menu loaded. Click PLAY or QUIT.");
    }

    @Override
    public void update(float dt) {
        uiManager.update(dt, window.getWidth(), window.getHeight());
    }

    @Override
    public void render() {
        int w = window.getWidth();
        int h = window.getHeight();
        camera.adjustProjection(w, h);

        batchRenderer.setProjection(camera.getProjectionViewMatrix());
        batchRenderer.beginBatch();

        // Title
        TextRenderer.drawTextFullyCentered(batchRenderer, titleFont,
                "TZAMAL ENGINE", 0f, 120f, new Vector4f(1f, 0.8f, 0.2f, 1f));

        // Subtitle
        TextRenderer.drawTextFullyCentered(batchRenderer, buttonFont,
                "Phase 7 Demo", 0f, 70f, new Vector4f(0.7f, 0.7f, 0.7f, 0.8f));

        // UI elements
        uiManager.render(batchRenderer, buttonFont);

        batchRenderer.endBatch();
        batchRenderer.flush();
    }

    @Override
    public void dispose() {
        if (batchRenderer != null) {
            batchRenderer.dispose();
        }
    }
}
