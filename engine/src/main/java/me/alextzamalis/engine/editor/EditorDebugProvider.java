package me.alextzamalis.engine.editor;

/**
 * Optional runtime debug UI rendered by the editor overlay (F1).
 * Game screens register a provider while active.
 */
public interface EditorDebugProvider {

    void renderWaveDebug();

    void renderCombatDebug();

    void renderLootDebug();

    void renderSettingsDebug();
}
