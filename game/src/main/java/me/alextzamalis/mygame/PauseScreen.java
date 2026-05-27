package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.screen.GameScreen;
import me.alextzamalis.engine.ui.UIManager;
import me.alextzamalis.engine.ui.UISpriteElement;
import me.alextzamalis.engine.ui.UITexturedButton;
import me.alextzamalis.mygame.data.GameAudio;
import me.alextzamalis.mygame.data.GameUiAssets;

import org.joml.Vector4f;

public class PauseScreen extends GameScreen {

    private Window window;
    private BatchRenderer batchRenderer;
    private Camera2D camera;
    private Font titleFont;
    private Font buttonFont;
    private UIManager uiManager;
    private UISpriteElement dialogBg;

    @Override
    public void init(Window window) {
        this.window = window;
        GameUiAssets.load();

        batchRenderer = new BatchRenderer(AssetManager.getOrLoadDefaultShader());
        camera = new Camera2D(window.getWidth(), window.getHeight());

        titleFont = GameUiAssets.loadScaledUiFont(32f);
        buttonFont = GameUiAssets.loadScaledUiFont(14f);

        uiManager = new UIManager();

        dialogBg = new UISpriteElement(
                -GameUiAssets.s(240f), -GameUiAssets.s(120f),
                GameUiAssets.s(480f), GameUiAssets.s(240f), GameUiAssets.dialogMediumSprite());
        uiManager.addElement(dialogBg);

        float btnW = GameUiAssets.s(220f);
        float btnH = GameUiAssets.s(50f);
        float btnX = -btnW / 2f;

        UITexturedButton resumeBtn = new UITexturedButton(btnX, 30f, btnW, btnH, "RESUME");
        GameUiAssets.styleGreenButton(resumeBtn);
        resumeBtn.setOnClick(() -> screenManager.popScreen());
        uiManager.addElement(resumeBtn);

        UITexturedButton menuBtn = new UITexturedButton(btnX, -40f, btnW, btnH, "MAIN MENU");
        GameUiAssets.styleRedButton(menuBtn);
        menuBtn.setOnClick(this::returnToMainMenu);
        uiManager.addElement(menuBtn);

        Logger.info("Pause", "Pause screen initialised.");
    }

    private void returnToMainMenu() {
        screenManager.popScreen();
        screenManager.swapScreen(new MainMenuScreen());
    }

    @Override
    public void update(float dt) {
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            screenManager.popScreen();
            return;
        }
        uiManager.update(dt, window.getWidth(), window.getHeight());
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

        batchRenderer.drawQuad(-halfW, -halfH, w, h,
                new Vector4f(0f, 0f, 0f, 0.5f));

        uiManager.render(batchRenderer, buttonFont);

        TextRenderer.drawTextFullyCentered(batchRenderer, titleFont, "PAUSED",
                0f, 100f, new Vector4f(0.9f, 0.3f, 0.3f, 1f));

        batchRenderer.endBatch();
        batchRenderer.flush();
    }

    @Override
    public void onEnter() {
        GameAudio.pauseMusic();
    }

    @Override
    public void onExit() {
        GameAudio.resumeMusic();
    }

    @Override
    public void dispose() {
        if (batchRenderer != null) {
            batchRenderer.dispose();
        }
    }

    @Override
    public boolean isTransparent() {
        return true;
    }
}
