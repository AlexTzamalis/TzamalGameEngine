package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.screen.GameScreen;
import me.alextzamalis.engine.ui.UICycleButton;
import me.alextzamalis.engine.ui.UIManager;
import me.alextzamalis.engine.ui.UISpriteElement;
import me.alextzamalis.engine.ui.UITexturedButton;
import me.alextzamalis.engine.ui.UIToggle;
import me.alextzamalis.engine.audio.AudioManager;
import me.alextzamalis.mygame.data.FpsPresets;
import me.alextzamalis.mygame.data.GameAudio;
import me.alextzamalis.mygame.data.GameSettings;
import me.alextzamalis.mygame.data.GameUiAssets;
import me.alextzamalis.mygame.data.ResolutionPresets;
import me.alextzamalis.mygame.data.SettingsStore;

import org.joml.Vector4f;

public class SettingsScreen extends GameScreen {

    private enum SettingsTab {
        DISPLAY,
        AUDIO
    }

    private static final float PANEL_W = GameUiAssets.s(420f);
    private static final float PANEL_H = GameUiAssets.s(480f);
    private static final float ROW_W = GameUiAssets.s(300f);
    private static final float ROW_H = GameUiAssets.s(44f);
    private static final float STEP_BTN = GameUiAssets.s(44f);

    private Window window;
    private BatchRenderer batch;
    private Camera2D camera;
    private Font titleFont;
    private Font bodyFont;
    private Font smallFont;
    private UIManager uiManager;
    private UISpriteElement panelBg;
    private GameSettings settings;
    private SettingsTab activeTab = SettingsTab.DISPLAY;

    private UITexturedButton displayTabBtn;
    private UITexturedButton audioTabBtn;

    private UICycleButton resolutionRow;
    private UICycleButton fpsRow;
    private UIToggle fullscreenToggle;

    private UITexturedButton masterVolDown;
    private UITexturedButton masterVolUp;
    private UITexturedButton musicVolDown;
    private UITexturedButton musicVolUp;
    private UITexturedButton sfxVolDown;
    private UITexturedButton sfxVolUp;

    @Override
    public void init(Window window) {
        this.window = window;
        GameUiAssets.load();
        settings = SettingsStore.load();
        settings.resolutionIndex = ResolutionPresets.clampIndex(settings.resolutionIndex);

        batch = new BatchRenderer(AssetManager.getOrLoadDefaultShader());
        camera = new Camera2D(window.getWidth(), window.getHeight());
        titleFont = GameUiAssets.loadScaledUiFont(24f);
        bodyFont = GameUiAssets.loadScaledUiFont(14f);
        smallFont = GameUiAssets.loadScaledUiFont(11f);
        uiManager = new UIManager();

        panelBg = new UISpriteElement(-PANEL_W / 2f, -PANEL_H / 2f, PANEL_W, PANEL_H,
                GameUiAssets.dialogMediumSprite());
        GameUiAssets.applyPanelNineSlice(panelBg, "settingsPanel");
        uiManager.addElement(panelBg);

        float tabW = GameUiAssets.s(120f);
        float tabH = GameUiAssets.s(36f);
        float tabY = PANEL_H / 2f - GameUiAssets.s(52f);
        displayTabBtn = new UITexturedButton(-tabW - 6f, tabY, tabW, tabH, "DISPLAY");
        GameUiAssets.styleMenuButton(displayTabBtn, false);
        displayTabBtn.setOnClick(() -> switchTab(SettingsTab.DISPLAY));
        uiManager.addElement(displayTabBtn);

        audioTabBtn = new UITexturedButton(6f, tabY, tabW, tabH, "AUDIO");
        GameUiAssets.styleMenuButton(audioTabBtn, false);
        audioTabBtn.setOnClick(() -> switchTab(SettingsTab.AUDIO));
        uiManager.addElement(audioTabBtn);

        float rowX = -ROW_W / 2f;

        resolutionRow = new UICycleButton(rowX, GameUiAssets.s(40f), ROW_W, ROW_H);
        GameUiAssets.styleCycleRow(resolutionRow);
        resolutionRow.setOnClick(this::cycleResolution);
        uiManager.addElement(resolutionRow);

        fpsRow = new UICycleButton(rowX, GameUiAssets.s(-20f), ROW_W, ROW_H);
        GameUiAssets.styleCycleRow(fpsRow);
        fpsRow.setOnClick(this::cycleFps);
        uiManager.addElement(fpsRow);

        fullscreenToggle = new UIToggle(rowX, GameUiAssets.s(-80f), GameUiAssets.s(56f), GameUiAssets.s(24f));
        GameUiAssets.styleSettingsToggle(fullscreenToggle);
        fullscreenToggle.setValue(settings.fullscreen);
        fullscreenToggle.setOnChange(this::toggleFullscreen);
        uiManager.addElement(fullscreenToggle);

        float volRowY = GameUiAssets.s(50f);
        float volGap = GameUiAssets.s(56f);
        masterVolDown = makeVolButton(-ROW_W / 2f - STEP_BTN - 8f, volRowY, "-",
                () -> adjustMasterVolume(-0.1f));
        masterVolUp = makeVolButton(ROW_W / 2f + 8f, volRowY, "+",
                () -> adjustMasterVolume(0.1f));
        musicVolDown = makeVolButton(-ROW_W / 2f - STEP_BTN - 8f, volRowY - volGap, "-",
                () -> adjustMusicVolume(-0.1f));
        musicVolUp = makeVolButton(ROW_W / 2f + 8f, volRowY - volGap, "+",
                () -> adjustMusicVolume(0.1f));
        sfxVolDown = makeVolButton(-ROW_W / 2f - STEP_BTN - 8f, volRowY - volGap * 2f, "-",
                () -> adjustSfxVolume(-0.1f));
        sfxVolUp = makeVolButton(ROW_W / 2f + 8f, volRowY - volGap * 2f, "+",
                () -> adjustSfxVolume(0.1f));

        UITexturedButton backBtn = new UITexturedButton(
                -GameUiAssets.s(100f), -PANEL_H / 2f + GameUiAssets.s(24f),
                GameUiAssets.s(200f), GameUiAssets.s(48f), "BACK");
        GameUiAssets.styleMenuButton(backBtn, false);
        backBtn.setOnClick(() -> {
            GameAudio.playRandomUiClick();
            screenManager.swapScreen(new MainMenuScreen());
        });
        uiManager.addElement(backBtn);

        applyAudioVolumesFromSettings();
        refreshControlLabels();
        updateTabVisibility();
        Logger.info("Settings", "Settings screen loaded.");
    }

    private UITexturedButton makeVolButton(float x, float y, String label, Runnable onClick) {
        UITexturedButton btn = new UITexturedButton(x, y, STEP_BTN, STEP_BTN, label);
        GameUiAssets.styleBrownButton(btn);
        btn.setOnClick(onClick);
        uiManager.addElement(btn);
        return btn;
    }

    private void switchTab(SettingsTab tab) {
        activeTab = tab;
        updateTabVisibility();
        GameAudio.playRandomUiClick();
    }

    private void updateTabVisibility() {
        boolean display = activeTab == SettingsTab.DISPLAY;
        resolutionRow.setVisible(display);
        fpsRow.setVisible(display);
        fullscreenToggle.setVisible(display);

        boolean audio = activeTab == SettingsTab.AUDIO;
        masterVolDown.setVisible(audio);
        masterVolUp.setVisible(audio);
        musicVolDown.setVisible(audio);
        musicVolUp.setVisible(audio);
        sfxVolDown.setVisible(audio);
        sfxVolUp.setVisible(audio);
    }

    private void applyAudioVolumesFromSettings() {
        AudioManager.setMasterVolume(settings.masterVolume);
        AudioManager.setMusicVolume(settings.musicVolume);
        AudioManager.setSfxVolume(settings.sfxVolume);
    }

    private void adjustMasterVolume(float delta) {
        settings.masterVolume = clampVolume(settings.masterVolume + delta);
        AudioManager.setMasterVolume(settings.masterVolume);
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
    }

    private void adjustMusicVolume(float delta) {
        settings.musicVolume = clampVolume(settings.musicVolume + delta);
        AudioManager.setMusicVolume(settings.musicVolume);
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
    }

    private void adjustSfxVolume(float delta) {
        settings.sfxVolume = clampVolume(settings.sfxVolume + delta);
        AudioManager.setSfxVolume(settings.sfxVolume);
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
    }

    private static float clampVolume(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void cycleResolution() {
        settings.resolutionIndex = ResolutionPresets.nextIndex(settings.resolutionIndex);
        if (!settings.fullscreen) {
            window.setWindowedSize(
                    ResolutionPresets.getWidth(settings.resolutionIndex),
                    ResolutionPresets.getHeight(settings.resolutionIndex));
        }
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
        refreshControlLabels();
    }

    private void cycleFps() {
        int idx = FpsPresets.nextIndex(FpsPresets.indexOfValue(settings.targetFps));
        settings.targetFps = FpsPresets.getValue(idx);
        window.setTargetFps(settings.targetFps);
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
        refreshControlLabels();
    }

    private void toggleFullscreen() {
        settings.fullscreen = fullscreenToggle.getValue();
        window.setFullscreen(settings.fullscreen);
        SettingsStore.save(settings);
        GameAudio.playRandomUiClick();
        refreshControlLabels();
    }

    private void refreshControlLabels() {
        resolutionRow.setText(ResolutionPresets.getLabel(settings.resolutionIndex));
        int fpsIdx = FpsPresets.indexOfValue(settings.targetFps);
        fpsRow.setText("FPS: " + FpsPresets.getLabel(fpsIdx));
        fullscreenToggle.setLabel(settings.fullscreen ? "Fullscreen: Enabled" : "Fullscreen: Disabled");
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

        float halfW = w / 2f;
        float halfH = h / 2f;

        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();

        batch.drawQuad(-halfW, -halfH, w, h, new Vector4f(0.1f, 0.1f, 0.15f, 1f));

        uiManager.render(batch, bodyFont);

        float titleY = PANEL_H / 2f + GameUiAssets.s(36f);
        TextRenderer.drawTextFullyCentered(batch, titleFont, "SETTINGS",
                0f, titleY, new Vector4f(1f, 0.8f, 0.2f, 1f));

        Vector4f labelColor = new Vector4f(0.75f, 0.75f, 0.75f, 0.9f);
        if (activeTab == SettingsTab.DISPLAY) {
            TextRenderer.drawText(batch, smallFont, "Resolution",
                    -ROW_W / 2f - 4f, GameUiAssets.s(62f), labelColor);
            TextRenderer.drawText(batch, smallFont, "FPS",
                    -ROW_W / 2f - 4f, GameUiAssets.s(2f), labelColor);

            float ctrlX = -PANEL_W / 2f + GameUiAssets.s(24f);
            float ctrlY = -PANEL_H / 2f + GameUiAssets.s(120f);
            TextRenderer.drawText(batch, bodyFont, "Controls", ctrlX, ctrlY,
                    new Vector4f(0.8f, 0.8f, 0.8f, 0.9f));
            TextRenderer.drawText(batch, smallFont, "E - Inventory", ctrlX, ctrlY - GameUiAssets.s(22f), labelColor);
            TextRenderer.drawText(batch, smallFont, "ESC - Pause", ctrlX, ctrlY - GameUiAssets.s(38f), labelColor);
            TextRenderer.drawText(batch, smallFont, "F1 - Editor", ctrlX, ctrlY - GameUiAssets.s(54f), labelColor);
        } else {
            int masterPct = Math.round(settings.masterVolume * 100f);
            int musicPct = Math.round(settings.musicVolume * 100f);
            int sfxPct = Math.round(settings.sfxVolume * 100f);
            float volLabelX = -ROW_W / 2f - 4f;
            TextRenderer.drawText(batch, smallFont, "Master",
                    volLabelX, GameUiAssets.s(62f), labelColor);
            TextRenderer.drawTextCentered(batch, bodyFont, "Master " + masterPct + "%",
                    0f, GameUiAssets.s(50f), new Vector4f(1f, 1f, 1f, 0.9f));

            TextRenderer.drawText(batch, smallFont, "Music",
                    volLabelX, GameUiAssets.s(6f), labelColor);
            TextRenderer.drawTextCentered(batch, bodyFont, "Music " + musicPct + "%",
                    0f, GameUiAssets.s(-6f), new Vector4f(1f, 1f, 1f, 0.9f));

            TextRenderer.drawText(batch, smallFont, "SFX",
                    volLabelX, GameUiAssets.s(-50f), labelColor);
            TextRenderer.drawTextCentered(batch, bodyFont, "SFX " + sfxPct + "%",
                    0f, GameUiAssets.s(-62f), new Vector4f(1f, 1f, 1f, 0.9f));
        }

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
