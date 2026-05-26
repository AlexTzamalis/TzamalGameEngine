package me.alextzamalis.engine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.scene.GameObject;
import me.alextzamalis.engine.scene.Scene;

/**
 * Manages the editor overlay panels. Toggled with F1.
 * Game screens register their active Scene and Camera so the
 * editor can inspect and modify them at runtime.
 */
public class EditorManager {

    private boolean editorVisible = false;
    private Scene activeScene;
    private Camera2D activeCamera;
    private GameObject selectedObject;

    private final SceneHierarchyPanel hierarchyPanel = new SceneHierarchyPanel(this);
    private final InspectorPanel inspectorPanel = new InspectorPanel(this);

    // FPS tracking
    private float fpsTimer = 0f;
    private int frameCount = 0;
    private float displayFps = 0f;

    /** Called each frame from the game loop (between ImGui.newFrame and ImGui.render). */
    public void update() {
        if (Input.isKeyJustPressed(Input.KEY_F1)) {
            editorVisible = !editorVisible;
            if (editorVisible) {
                System.out.println("[Editor] Editor opened (F1)");
            } else {
                System.out.println("[Editor] Editor closed (F1)");
            }
        }

        if (!editorVisible) {
            return;
        }

        float dt = ImGui.getIO().getDeltaTime();
        fpsTimer += dt;
        frameCount++;
        if (fpsTimer >= 0.5f) {
            displayFps = frameCount / fpsTimer;
            frameCount = 0;
            fpsTimer = 0f;
        }

        renderFpsOverlay();
        hierarchyPanel.render(activeScene);
        inspectorPanel.render(selectedObject, activeCamera);
    }

    private void renderFpsOverlay() {
        int flags = ImGuiWindowFlags.NoDecoration
                | ImGuiWindowFlags.AlwaysAutoResize
                | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoFocusOnAppearing
                | ImGuiWindowFlags.NoNav;

        ImGui.setNextWindowPos(10f, 10f, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0.5f);

        if (ImGui.begin("FPS", flags)) {
            ImGui.text(String.format("FPS: %.1f", displayFps));
            ImGui.text(String.format("Frame: %.2f ms", 1000f / Math.max(displayFps, 1f)));
        }
        ImGui.end();
    }

    public void setActiveScene(Scene scene) { this.activeScene = scene; }
    public void setActiveCamera(Camera2D camera) { this.activeCamera = camera; }
    public GameObject getSelectedObject() { return selectedObject; }
    public void setSelectedObject(GameObject obj) { this.selectedObject = obj; }
}
