package me.alextzamalis.engine.editor;

import imgui.ImGui;
import imgui.type.ImInt;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.scene.GameObject;
import me.alextzamalis.engine.scene.Sprite;
import me.alextzamalis.engine.scene.Transform;
import org.joml.Vector2f;

/**
 * ImGui inspector panel for editing the selected GameObject's
 * properties and the active camera.
 */
public class InspectorPanel {

    private final EditorManager editor;

    // Reusable ImGui value holders to avoid per-frame allocation
    private final float[] floatVal = new float[1];
    private final float[] colorVal = new float[4];
    private final ImInt intVal = new ImInt();

    InspectorPanel(EditorManager editor) {
        this.editor = editor;
    }

    public void render(GameObject selected, Camera2D camera) {
        ImGui.begin("Inspector");

        if (selected != null) {
            renderGameObjectInspector(selected);
        } else {
            ImGui.text("No object selected");
        }

        ImGui.separator();
        ImGui.spacing();

        if (camera != null) {
            renderCameraControls(camera);
        }

        ImGui.end();
    }

    private void renderGameObjectInspector(GameObject go) {
        ImGui.text("Name: " + go.getName());
        ImGui.separator();

        Transform t = go.getTransform();

        // Position
        ImGui.text("Position");
        floatVal[0] = t.position.x;
        if (ImGui.dragFloat("X##pos", floatVal, 1f)) {
            t.position.x = floatVal[0];
        }
        floatVal[0] = t.position.y;
        if (ImGui.dragFloat("Y##pos", floatVal, 1f)) {
            t.position.y = floatVal[0];
        }

        ImGui.spacing();

        // Scale
        ImGui.text("Scale");
        floatVal[0] = t.scale.x;
        if (ImGui.dragFloat("W##scale", floatVal, 0.5f)) {
            t.scale.x = floatVal[0];
        }
        floatVal[0] = t.scale.y;
        if (ImGui.dragFloat("H##scale", floatVal, 0.5f)) {
            t.scale.y = floatVal[0];
        }

        ImGui.spacing();

        // Rotation
        floatVal[0] = t.rotation;
        if (ImGui.dragFloat("Rotation", floatVal, 0.5f)) {
            t.rotation = floatVal[0];
        }

        ImGui.spacing();

        // Z-Index
        intVal.set(go.getZIndex());
        if (ImGui.inputInt("Z-Index", intVal)) {
            go.setZIndex(intVal.get());
        }

        // Sprite color (if present)
        Sprite sprite = go.getSprite();
        if (sprite != null) {
            ImGui.spacing();
            ImGui.separator();
            ImGui.text("Sprite");

            colorVal[0] = sprite.color.x;
            colorVal[1] = sprite.color.y;
            colorVal[2] = sprite.color.z;
            colorVal[3] = sprite.color.w;
            if (ImGui.colorEdit4("Color", colorVal)) {
                sprite.color.set(colorVal[0], colorVal[1], colorVal[2], colorVal[3]);
            }
        }
    }

    private void renderCameraControls(Camera2D camera) {
        ImGui.text("Camera");

        Vector2f pos = camera.getPosition();
        floatVal[0] = pos.x;
        if (ImGui.dragFloat("Cam X", floatVal, 1f)) {
            pos.x = floatVal[0];
            camera.setPosition(pos);
        }
        floatVal[0] = pos.y;
        if (ImGui.dragFloat("Cam Y", floatVal, 1f)) {
            pos.y = floatVal[0];
            camera.setPosition(pos);
        }

        floatVal[0] = camera.getZoom();
        if (ImGui.sliderFloat("Zoom", floatVal, 0.1f, 5.0f)) {
            camera.setZoom(floatVal[0]);
        }

        floatVal[0] = camera.getRotation();
        if (ImGui.dragFloat("Rotation##cam", floatVal, 0.5f)) {
            camera.setRotation(floatVal[0]);
        }
    }
}
