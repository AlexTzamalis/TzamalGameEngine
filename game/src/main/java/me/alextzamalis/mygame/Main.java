package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.audio.AudioManager;
import me.alextzamalis.engine.screen.ScreenManager;
import me.alextzamalis.mygame.data.GameSettings;
import me.alextzamalis.mygame.data.GameAudio;
import me.alextzamalis.mygame.data.ResolutionPresets;
import me.alextzamalis.mygame.data.SettingsStore;

/**
 * Entry point for the TzamalGameEngine demo. Bootstraps the window
 * and delegates all lifecycle calls to a {@link ScreenManager}.
 *
 * @author Alexandros Tzamalis
 */
public class Main implements Window.GameLifecycle {

    private Window window;
    private ScreenManager screenManager;
    private GameSettings settings;

    /**
     * JVM entry point.
     *
     * @param args command line args (unused).
     */
    public static void main(String[] args) {
        GameSettings settings = SettingsStore.load();
        settings.resolutionIndex = ResolutionPresets.clampIndex(settings.resolutionIndex);
        int w = ResolutionPresets.getWidth(settings.resolutionIndex);
        int h = ResolutionPresets.getHeight(settings.resolutionIndex);
        Window window = new Window("Oil Protection Defense", w, h);
        window.run(new Main());
    }

    @Override
    public void init(Window window) {
        this.window = window;
        settings = SettingsStore.load();
        settings.resolutionIndex = ResolutionPresets.clampIndex(settings.resolutionIndex);

        AudioManager.setMasterVolume(settings.masterVolume);
        AudioManager.setMusicVolume(settings.musicVolume);
        AudioManager.setSfxVolume(settings.sfxVolume);
        window.setTargetFps(settings.targetFps);
        window.setResizable(false);
        window.setWindowedSize(
                ResolutionPresets.getWidth(settings.resolutionIndex),
                ResolutionPresets.getHeight(settings.resolutionIndex));
        window.setFullscreen(settings.fullscreen);
        GameAudio.load();

        screenManager = new ScreenManager(window);
        screenManager.pushScreen(new MainMenuScreen());
    }

    @Override
    public void update(float deltaTime) {
        screenManager.update(deltaTime);
    }

    @Override
    public void render() {
        screenManager.render();
    }

    @Override
    public void dispose() {
        GameAudio.stopMusic();
        if (screenManager != null) {
            screenManager.dispose();
        }
        AssetManager.disposeAll();
    }
}
