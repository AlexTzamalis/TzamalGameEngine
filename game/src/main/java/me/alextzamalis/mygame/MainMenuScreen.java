package me.alextzamalis.mygame;



import me.alextzamalis.engine.Window;

import me.alextzamalis.engine.assets.AssetManager;

import me.alextzamalis.engine.core.Logger;

import me.alextzamalis.engine.graphics.BatchRenderer;

import me.alextzamalis.engine.graphics.Camera2D;

import me.alextzamalis.engine.graphics.text.Font;

import me.alextzamalis.engine.graphics.text.TextRenderer;

import me.alextzamalis.engine.screen.GameScreen;

import me.alextzamalis.engine.ui.UIManager;

import me.alextzamalis.engine.ui.UITexturedButton;

import me.alextzamalis.mygame.data.GameAudio;

import me.alextzamalis.mygame.data.GameUiAssets;



import org.joml.Vector4f;



public class MainMenuScreen extends GameScreen {



    private static final float BTN_W = GameUiAssets.s(260f);
    private static final float BTN_H = GameUiAssets.s(48f);
    private static final float BTN_GAP = GameUiAssets.s(16f);



    private Window window;

    private BatchRenderer batchRenderer;

    private Camera2D camera;

    private Font titleFont;

    private Font buttonFont;

    private UIManager uiManager;



    @Override

    public void init(Window window) {

        this.window = window;

        GameUiAssets.load();



        batchRenderer = new BatchRenderer(AssetManager.getOrLoadDefaultShader());

        camera = new Camera2D(window.getWidth(), window.getHeight());



        titleFont = GameUiAssets.loadScaledUiFont(28f);
        buttonFont = GameUiAssets.loadScaledUiFont(14f);



        uiManager = new UIManager();



        float btnX = -BTN_W / 2f;

        float playY = BTN_GAP;

        float settingsY = playY - BTN_H - BTN_GAP;

        float closeY = settingsY - BTN_H - BTN_GAP;



        UITexturedButton playBtn = new UITexturedButton(btnX, playY, BTN_W, BTN_H, "");

        GameUiAssets.styleMenuButton(playBtn, true);

        playBtn.setOnClick(() -> {

            GameAudio.playRandomUiClick();

            screenManager.swapScreen(new OPDPlayScreen());

        });

        uiManager.addElement(playBtn);



        UITexturedButton settingsBtn = new UITexturedButton(btnX, settingsY, BTN_W, BTN_H, "SETTINGS");

        GameUiAssets.styleMenuButton(settingsBtn, false);

        settingsBtn.setOnClick(() -> {

            GameAudio.playRandomUiClick();

            screenManager.swapScreen(new SettingsScreen());

        });

        uiManager.addElement(settingsBtn);



        UITexturedButton closeBtn = new UITexturedButton(btnX, closeY, BTN_W, BTN_H, "CLOSE");

        GameUiAssets.styleMenuButton(closeBtn, false);

        closeBtn.setOnClick(() -> {

            GameAudio.playRandomUiClick();

            window.requestClose();

        });

        uiManager.addElement(closeBtn);



        Logger.info("Menu", "Main menu loaded.");

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



        TextRenderer.drawTextFullyCentered(batchRenderer, titleFont,

                "OIL PROTECTION", 0f, GameUiAssets.s(120f), new Vector4f(1f, 0.8f, 0.2f, 1f));

        TextRenderer.drawTextFullyCentered(batchRenderer, titleFont,

                "DEFENSE", 0f, GameUiAssets.s(88f), new Vector4f(1f, 0.8f, 0.2f, 1f));



        uiManager.render(batchRenderer, buttonFont);



        batchRenderer.endBatch();

        batchRenderer.flush();

    }



    @Override

    public void onEnter() {

        GameAudio.playMenuMusic();

    }



    @Override

    public void onExit() {

        GameAudio.stopMusic();

    }



    @Override

    public void dispose() {

        if (batchRenderer != null) {

            batchRenderer.dispose();

        }

    }

}

