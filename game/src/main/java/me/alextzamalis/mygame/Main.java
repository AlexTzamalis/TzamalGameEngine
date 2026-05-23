package me.alextzamalis.mygame;

import me.alextzamalis.engine.AssetManager;
import me.alextzamalis.engine.BatchRenderer;
import me.alextzamalis.engine.Input;
import me.alextzamalis.engine.Shader;
import me.alextzamalis.engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Entry point for a demo game using the Tzamal Game Engine.
 *
 * <p>This class implements the engine's {@link Window.GameLifecycle}
 * contract and demonstrates Phase 2 rendering: a batch renderer
 * drawing colored quads with the default shader.</p>
 *
 * <h2>How to run</h2>
 * <pre>{@code
 *   ./gradlew :game:run
 * }</pre>
 *
 * <h2>What you should see</h2>
 * <p>A window with several colored quads rendered via the batch
 * renderer. Press <strong>Escape</strong> to close.</p>
 *
 * @author Alexandros Tzamalis
 * @see Window
 * @see Window.GameLifecycle
 * @see BatchRenderer
 */
public class Main implements Window.GameLifecycle {

    private Window window;
    private BatchRenderer batchRenderer;

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

        // Upload the texture sampler uniform array once at init
        defaultShader.bind();
        int[] slots = new int[16];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        for (int i = 0; i < slots.length; i++) {
            defaultShader.uploadTexture("uTextures[" + i + "]", i);
        }
        defaultShader.unbind();

        batchRenderer = new BatchRenderer(defaultShader);

        System.out.println("[Game] init() complete. Shader and batch renderer ready.");
    }

    @Override
    public void update(float deltaTime) {
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            System.out.println("[Game] Escape pressed. Requesting window close.");
            window.requestClose();
        }
    }

    @Override
    public void render() {
        Matrix4f projection = new Matrix4f().ortho(
                0f, window.getWidth(),
                0f, window.getHeight(),
                -1f, 1f
        );
        batchRenderer.setProjection(projection);

        batchRenderer.beginBatch();

        // Draw a few colored quads to prove the batch renderer works
        batchRenderer.drawQuad(50f,  50f,  200f, 200f, new Vector4f(1f, 0.2f, 0.2f, 1f));
        batchRenderer.drawQuad(300f, 50f,  200f, 200f, new Vector4f(0.2f, 1f, 0.2f, 1f));
        batchRenderer.drawQuad(550f, 50f,  200f, 200f, new Vector4f(0.2f, 0.2f, 1f, 1f));
        batchRenderer.drawQuad(175f, 300f, 200f, 200f, new Vector4f(1f, 1f, 0.2f, 1f));
        batchRenderer.drawQuad(425f, 300f, 200f, 200f, new Vector4f(0.2f, 1f, 1f, 1f));

        batchRenderer.endBatch();
        batchRenderer.flush();
    }

    @Override
    public void dispose() {
        if (batchRenderer != null) {
            batchRenderer.dispose();
        }
        AssetManager.disposeAll();
        System.out.println("[Game] dispose() complete. All resources freed.");
    }
}
