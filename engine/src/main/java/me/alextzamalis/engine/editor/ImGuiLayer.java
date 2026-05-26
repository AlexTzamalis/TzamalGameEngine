package me.alextzamalis.engine.editor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import me.alextzamalis.engine.core.Input;

public class ImGuiLayer {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    /**
     * Initializes ImGui context, GLFW backend, and GL3 backend.
     * Must be called after GLFW window creation and after Input.install().
     * The GLFW backend is initialized with installCallbacks=true, which
     * chains with Input's existing callbacks.
     */
    public void init(long windowHandle) {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 330 core");
    }

    /**
     * Starts a new ImGui frame. Call before any ImGui widget commands.
     * Also updates Input's ImGui capture flags.
     */
    public void beginFrame() {
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();

        ImGuiIO io = ImGui.getIO();
        Input.setImGuiCapture(io.getWantCaptureMouse(), io.getWantCaptureKeyboard());
    }

    /**
     * Finalizes and renders the ImGui frame. Call after all widget commands.
     */
    public void endFrame() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    /** Disposes all ImGui resources. */
    public void dispose() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }
}
