package me.alextzamalis.engine.editor;

import imgui.ImGui;
import me.alextzamalis.engine.scene.GameObject;
import me.alextzamalis.engine.scene.Scene;
import me.alextzamalis.engine.scene.Transform;

import java.util.List;

/**
 * ImGui panel listing all GameObjects in the active scene.
 * Click to select, buttons to add/delete objects.
 */
public class SceneHierarchyPanel {

    private final EditorManager editor;
    private int newObjectCounter = 0;

    SceneHierarchyPanel(EditorManager editor) {
        this.editor = editor;
    }

    public void render(Scene scene) {
        ImGui.begin("Scene Hierarchy");

        if (scene == null) {
            ImGui.text("No active scene");
            ImGui.end();
            return;
        }

        List<GameObject> objects = scene.getGameObjects();
        for (int i = 0; i < objects.size(); i++) {
            GameObject go = objects.get(i);
            boolean selected = (go == editor.getSelectedObject());

            if (ImGui.selectable(go.getName() + "##" + i, selected)) {
                editor.setSelectedObject(go);
            }
        }

        ImGui.separator();

        if (ImGui.button("Add GameObject")) {
            newObjectCounter++;
            GameObject newGo = new GameObject("new_object_" + newObjectCounter, new Transform());
            scene.addGameObject(newGo);
            editor.setSelectedObject(newGo);
        }

        ImGui.sameLine();

        if (ImGui.button("Delete Selected")) {
            GameObject sel = editor.getSelectedObject();
            if (sel != null) {
                scene.removeGameObject(sel);
                editor.setSelectedObject(null);
            }
        }

        ImGui.separator();
        ImGui.text("Objects: " + objects.size());

        ImGui.end();
    }
}
