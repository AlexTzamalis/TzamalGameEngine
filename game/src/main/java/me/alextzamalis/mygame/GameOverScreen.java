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
import me.alextzamalis.mygame.data.GameUiAssets;

import org.joml.Vector4f;

/**
 * Displays the final score and wave reached after the player dies.
 * ENTER retries, ESC returns to the main menu.
 */
public class GameOverScreen extends GameScreen {

    private final int finalScore;
    private final int finalWave;

    private Window window;
    private BatchRenderer batch;
    private Camera2D camera;
    private Font titleFont;
    private Font bodyFont;
    private UIManager uiManager;
    private UISpriteElement dialogBg;

    public GameOverScreen(int finalScore, int finalWave) {
        this.finalScore = finalScore;
        this.finalWave = finalWave;
    }

    @Override
    public void init(Window window) {
        this.window = window;
        GameUiAssets.load();

        batch = new BatchRenderer(AssetManager.getOrLoadDefaultShader());
        camera = new Camera2D(window.getWidth(), window.getHeight());

        titleFont = GameUiAssets.loadScaledUiFont(28f);
        bodyFont = GameUiAssets.loadScaledUiFont(14f);
        uiManager = new UIManager();

        dialogBg = new UISpriteElement(
                -GameUiAssets.s(240f), -GameUiAssets.s(140f),
                GameUiAssets.s(480f), GameUiAssets.s(280f), GameUiAssets.dialogMediumSprite());
        uiManager.addElement(dialogBg);

        UITexturedButton retryBtn = new UITexturedButton(
                -GameUiAssets.s(110f), -GameUiAssets.s(90f), GameUiAssets.s(220f), GameUiAssets.s(44f), "RETRY");
        GameUiAssets.styleGreenButton(retryBtn);
        retryBtn.setOnClick(() -> screenManager.swapScreen(new OPDPlayScreen()));
        uiManager.addElement(retryBtn);

        UITexturedButton menuBtn = new UITexturedButton(-110f, -150f, 220f, 44f, "MAIN MENU");
        GameUiAssets.styleBrownButton(menuBtn);
        menuBtn.setOnClick(() -> screenManager.swapScreen(new MainMenuScreen()));
        uiManager.addElement(menuBtn);

        Logger.info("GameOver", "Game Over - Score: " + finalScore + " Wave: " + finalWave);
    }

    @Override
    public void update(float dt) {
        if (Input.isKeyJustPressed(Input.KEY_ENTER)) {
            screenManager.swapScreen(new OPDPlayScreen());
        }
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            screenManager.swapScreen(new MainMenuScreen());
        }
        uiManager.update(dt, window.getWidth(), window.getHeight());
    }

    @Override
    public void render() {
        int w = window.getWidth();
        int h = window.getHeight();
        camera.adjustProjection(w, h);

        float halfW = w / 2f;
        float halfH = h / 2f;

        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();

        batch.drawQuad(-halfW, -halfH, w, h,
                new Vector4f(0.08f, 0.02f, 0.02f, 1.0f));

        uiManager.render(batch, bodyFont);

        TextRenderer.drawTextFullyCentered(batch, titleFont, "GAME OVER",
                0f, 80f, new Vector4f(0.9f, 0.2f, 0.2f, 1.0f));

        TextRenderer.drawTextFullyCentered(batch, bodyFont,
                "SCORE: " + finalScore,
                0f, 20f, new Vector4f(1f, 1f, 1f, 0.9f));

        TextRenderer.drawTextFullyCentered(batch, bodyFont,
                "WAVE REACHED: " + finalWave,
                0f, -10f, new Vector4f(0.8f, 0.8f, 0.8f, 0.8f));

        batch.endBatch();
        batch.flush();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
    }
}
