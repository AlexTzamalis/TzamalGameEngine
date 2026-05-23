package me.alextzamalis.mygame;

import me.alextzamalis.engine.AssetManager;
import me.alextzamalis.engine.BatchRenderer;
import me.alextzamalis.engine.Camera2D;
import me.alextzamalis.engine.GameObject;
import me.alextzamalis.engine.Input;
import me.alextzamalis.engine.Scene;
import me.alextzamalis.engine.Shader;
import me.alextzamalis.engine.Sprite;
import me.alextzamalis.engine.Transform;
import me.alextzamalis.engine.Window;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Entry point for a demo game using the Tzamal Game Engine.
 *
 * <p>Demonstrates Phase 3 features: Camera2D with WASD movement and
 * zoom, Scene management with multiple GameObjects sorted by z-index,
 * and the Sprite/Transform component model.</p>
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li>WASD - move camera</li>
 *   <li>Q/E - zoom in/out</li>
 *   <li>Escape - exit</li>
 * </ul>
 *
 * @author Alexandros Tzamalis
 */
public class Main implements Window.GameLifecycle {

    private static final float CAMERA_SPEED = 300f;
    private static final float ZOOM_SPEED = 1.0f;

    private Window window;
    private BatchRenderer batchRenderer;
    private Camera2D camera;
    private Scene scene;

    private int lastWidth;
    private int lastHeight;

    // Application entry point

    /**
     * JVM entry point.
     *
     * @param args command line args (unused).
     */
    public static void main(String[] args) {
        Window window = new Window("TzamalGameEngine Demo", 1280, 720);
        window.run(new Main());
    }

    // GameLifecycle implementation

    @Override
    public void init(Window window) {
        this.window = window;

        Shader defaultShader = Shader.fromResource("/shaders/default.vert", "/shaders/default.frag");
        AssetManager.addShader("default", defaultShader);

        defaultShader.bind();
        for (int i = 0; i < 16; i++) {
            defaultShader.uploadTexture("uTextures[" + i + "]", i);
        }
        defaultShader.unbind();

        batchRenderer = new BatchRenderer(defaultShader);

        camera = new Camera2D(window.getWidth(), window.getHeight());
        lastWidth = window.getWidth();
        lastHeight = window.getHeight();

        scene = new Scene();
        createDemoObjects();

        // console help messages for inputs
        System.out.println("[Game] Phase 3 demo initialised.");
        System.out.println("[Game] Controls:");
        System.out.println(" WASD - Move camera");
        System.out.println(" Q/E  - Zoom in / out");
        System.out.println(" ESC  - Exit");
    }

    @Override
    public void update(float deltaTime) {
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            window.requestClose();
            return;
        }

        // Camera movement
        Vector2f camPos = camera.getPosition();
        float speed = CAMERA_SPEED * deltaTime;

        if (Input.isKeyPressed(Input.KEY_W)) {
            camPos.y += speed;
        }
        if (Input.isKeyPressed(Input.KEY_S)) {
            camPos.y -= speed;
        }
        if (Input.isKeyPressed(Input.KEY_A)) {
            camPos.x -= speed;
        }
        if (Input.isKeyPressed(Input.KEY_D)) {
            camPos.x += speed;
        }
        camera.setPosition(camPos);

        // Zoom control
        float zoom = camera.getZoom();
        if (Input.isKeyPressed(Input.KEY_Q)) {
            zoom += ZOOM_SPEED * deltaTime;
        }
        if (Input.isKeyPressed(Input.KEY_E)) {
            zoom -= ZOOM_SPEED * deltaTime;
        }
        if (zoom < 0.1f) {
            zoom = 0.1f;
        }
        if (zoom > 5.0f) {
            zoom = 5.0f;
        }
        camera.setZoom(zoom);

        // Handle window resize
        if (window.getWidth() != lastWidth || window.getHeight() != lastHeight) {
            lastWidth = window.getWidth();
            lastHeight = window.getHeight();
            camera.adjustProjection(lastWidth, lastHeight);
        }

        scene.update(deltaTime);
    }

    @Override
    public void render() {
        scene.render(batchRenderer, camera);
    }

    @Override
    public void dispose() {
        if (scene != null) {
            scene.dispose();
        }
        if (batchRenderer != null) {
            batchRenderer.dispose();
        }
        AssetManager.disposeAll();
        System.out.println("[Game] dispose() complete. All resources freed.");
    }

    // Demo scene setup with random and demo colored cubes
    // Basicly Testing the shaders and the color transitioning etc.
    private void createDemoObjects() {
        GameObject ground = new GameObject("ground",
                new Transform(new Vector2f(-400f, -200f), new Vector2f(800f, 50f)));
        ground.addSprite(new Sprite(new Vector4f(0.3f, 0.6f, 0.2f, 1f)));
        ground.setZIndex(0);
        scene.addGameObject(ground);

        GameObject red = new GameObject("red_block",
                new Transform(new Vector2f(-100f, -140f), new Vector2f(80f, 80f)));
        red.addSprite(new Sprite(new Vector4f(0.9f, 0.2f, 0.2f, 1f)));
        red.setZIndex(2);
        scene.addGameObject(red);

        GameObject blue = new GameObject("blue_block",
                new Transform(new Vector2f(-60f, -120f), new Vector2f(80f, 80f)));
        blue.addSprite(new Sprite(new Vector4f(0.2f, 0.3f, 0.9f, 0.85f)));
        blue.setZIndex(1);
        scene.addGameObject(blue);

        GameObject yellow = new GameObject("yellow_block",
                new Transform(new Vector2f(50f, -140f), new Vector2f(60f, 60f)));
        yellow.addSprite(new Sprite(new Vector4f(1f, 0.9f, 0.1f, 1f)));
        yellow.setZIndex(2);
        scene.addGameObject(yellow);

        GameObject purple = new GameObject("purple_column",
                new Transform(new Vector2f(150f, -150f), new Vector2f(40f, 200f)));
        purple.addSprite(new Sprite(new Vector4f(0.6f, 0.2f, 0.8f, 1f)));
        purple.setZIndex(1);
        scene.addGameObject(purple);

        GameObject cyan = new GameObject("cyan_overlay",
                new Transform(new Vector2f(-200f, -50f), new Vector2f(150f, 150f)));
        cyan.addSprite(new Sprite(new Vector4f(0.1f, 0.8f, 0.8f, 0.5f)));
        cyan.setZIndex(3);
        scene.addGameObject(cyan);

        GameObject origin = new GameObject("origin_marker",
                new Transform(new Vector2f(-5f, -5f), new Vector2f(10f, 10f)));
        origin.addSprite(new Sprite(new Vector4f(1f, 1f, 1f, 1f)));
        origin.setZIndex(4);
        scene.addGameObject(origin);

        GameObject orange = new GameObject("orange_block",
                new Transform(new Vector2f(250f, -100f), new Vector2f(100f, 70f)));
        orange.addSprite(new Sprite(new Vector4f(1f, 0.5f, 0.1f, 1f)));
        orange.setZIndex(1);
        scene.addGameObject(orange);
    }
}
