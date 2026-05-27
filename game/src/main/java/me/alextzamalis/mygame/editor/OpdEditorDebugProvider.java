package me.alextzamalis.mygame.editor;

import imgui.ImGui;
import imgui.type.ImInt;
import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.audio.AudioManager;
import me.alextzamalis.engine.editor.EditorDebugProvider;
import me.alextzamalis.mygame.OPDPlayScreen;
import me.alextzamalis.mygame.data.FpsPresets;
import me.alextzamalis.mygame.data.GameSettings;
import me.alextzamalis.mygame.data.ResolutionPresets;
import me.alextzamalis.mygame.data.SettingsStore;

/**
 * ImGui debug panels for Oil Protection Defense gameplay.
 */
public class OpdEditorDebugProvider implements EditorDebugProvider {

    private final OPDPlayScreen screen;
    private final Window window;
    private GameSettings settings;
    private final ImInt bucksInput = new ImInt(50);

    public OpdEditorDebugProvider(OPDPlayScreen screen, Window window) {
        this.screen = screen;
        this.window = window;
        this.settings = SettingsStore.load();
    }

    @Override
    public void renderWaveDebug() {
        ImGui.text("Wave: " + screen.getDebugWaveNumber());
        ImGui.text("Active: " + screen.isDebugWaveActive());
        ImGui.text("Enemies left to spawn: " + screen.getDebugEnemiesToSpawn());
        ImGui.text("Spawn interval: " + String.format("%.2f s", screen.getDebugSpawnInterval()));
        ImGui.text("Stage: " + screen.getDebugStageName());
        ImGui.text("Theme transition: " + screen.isDebugThemeTransitionRunning());

        if (ImGui.button("Skip Wave")) {
            screen.debugSkipWave();
        }
        ImGui.sameLine();
        if (ImGui.button("Force Theme Change")) {
            screen.debugForceThemeTransition();
        }
    }

    @Override
    public void renderCombatDebug() {
        ImGui.text("Player HP: " + screen.getDebugHealth() + " / " + screen.getDebugMaxHealth());
        ImGui.text("Score: " + screen.getDebugScore());
        ImGui.text("Enemies alive: " + screen.getDebugEnemyCount());
        ImGui.text("Projectiles: " + screen.getDebugProjectileCount());

        for (String line : screen.getDebugEnemyLines()) {
            ImGui.textWrapped(line);
        }

        if (ImGui.button("Kill All Enemies")) {
            screen.debugKillAllEnemies();
        }
        ImGui.sameLine();
        if (ImGui.button("Spawn Random Enemy")) {
            screen.debugSpawnRandomEnemy();
        }
    }

    @Override
    public void renderLootDebug() {
        ImGui.text("Bucks: " + screen.getDebugBucks());
        ImGui.text("Setup phase: " + screen.isDebugSetupPhase());
        ImGui.text("Inventory open: " + screen.isDebugInventoryOpen());
        ImGui.text("Equipped weapons: " + screen.getDebugEquippedWeaponCount());

        for (String line : screen.getDebugInventoryLines()) {
            ImGui.textWrapped(line);
        }

        ImGui.inputInt("Add bucks", bucksInput);
        if (ImGui.button("Add Bucks")) {
            screen.debugAddBucks(Math.max(0, bucksInput.get()));
        }
    }

    @Override
    public void renderSettingsDebug() {
        settings = SettingsStore.load();
        ImGui.text(String.format("Volume: %.0f%%", settings.masterVolume * 100f));
        ImGui.text("Fullscreen: " + (window.isFullscreen() ? "ON" : "OFF"));
        ImGui.text("Resolution: " + ResolutionPresets.getLabel(settings.resolutionIndex));
        ImGui.text("Target FPS: " + (settings.targetFps <= 0 ? "MAX" : settings.targetFps));
        ImGui.text(String.format("Window: %dx%d", window.getWidth(), window.getHeight()));

        if (ImGui.button("Vol -")) {
            settings.masterVolume = Math.max(0f, settings.masterVolume - 0.1f);
            AudioManager.setMasterVolume(settings.masterVolume);
            SettingsStore.save(settings);
        }
        ImGui.sameLine();
        if (ImGui.button("Vol +")) {
            settings.masterVolume = Math.min(1f, settings.masterVolume + 0.1f);
            AudioManager.setMasterVolume(settings.masterVolume);
            SettingsStore.save(settings);
        }

        if (ImGui.button("Toggle Fullscreen")) {
            settings.fullscreen = !settings.fullscreen;
            window.setFullscreen(settings.fullscreen);
            SettingsStore.save(settings);
        }

        if (ImGui.button("Cycle Resolution")) {
            settings.resolutionIndex = ResolutionPresets.nextIndex(settings.resolutionIndex);
            if (!settings.fullscreen) {
                window.setWindowedSize(
                        ResolutionPresets.getWidth(settings.resolutionIndex),
                        ResolutionPresets.getHeight(settings.resolutionIndex));
            }
            SettingsStore.save(settings);
        }

        if (ImGui.button("Cycle FPS")) {
            int idx = FpsPresets.nextIndex(FpsPresets.indexOfValue(settings.targetFps));
            settings.targetFps = FpsPresets.getValue(idx);
            window.setTargetFps(settings.targetFps);
            SettingsStore.save(settings);
        }
    }
}
