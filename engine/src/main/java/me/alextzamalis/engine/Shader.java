package me.alextzamalis.engine;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Compiles, links, and manages an OpenGL shader program.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Construct with vertex and fragment GLSL source strings, or use
 *       the factory method {@link #fromResource(String, String)} to load
 *       from classpath resources.</li>
 *   <li>The constructor compiles both shader stages and links them into
 *       a program. If compilation or linking fails, the constructor
 *       throws {@link RuntimeException} with the driver's info log so
 *       you get immediate feedback.</li>
 *   <li>Call {@link #bind()} before uploading uniforms or issuing draw
 *       calls that should use this program.</li>
 *   <li>Upload uniforms via the {@code upload*} methods. Each method
 *       requires the program to be bound first because
 *       {@code glUniform*} operates on the currently active program.</li>
 *   <li>Call {@link #unbind()} when done (optional but good practice
 *       to avoid accidental state leaks).</li>
 *   <li>Call {@link #dispose()} when the shader is no longer needed
 *       to free the GPU resource.</li>
 * </ol>
 *
 * <h2>Uniform location caching</h2>
 * <p>{@code glGetUniformLocation} is not free -- it performs a string
 * lookup on the driver side. This class caches the result in a
 * {@link HashMap} so repeated uploads to the same uniform name
 * only pay the lookup cost once.</p>
 *
 * @author Alexandros Tzamalis
 * @see BatchRenderer
 * @see AssetManager
 */
public class Shader {

    private final int programId;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    /**
     * Compiles vertex and fragment shaders from raw GLSL source strings
     * and links them into a shader program.
     *
     * @param vertexSource   GLSL source for the vertex stage.
     * @param fragmentSource GLSL source for the fragment stage.
     * @throws RuntimeException if compilation or linking fails.
     */
    public Shader(String vertexSource, String fragmentSource) {
        int vertexId = compileStage(GL_VERTEX_SHADER, vertexSource);
        int fragmentId = compileStage(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteShader(vertexId);
            glDeleteShader(fragmentId);
            glDeleteProgram(programId);
            throw new RuntimeException("Shader program linking failed:\n" + log);
        }

        // Shaders are linked into the program; the individual stage
        // objects are no longer needed and can be deleted immediately.
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    /**
     * Loads vertex and fragment shader source from classpath resources
     * and returns a compiled/linked {@link Shader}.
     *
     * <p>Resource paths are resolved via the engine's class loader, so
     * files placed under {@code engine/src/main/resources/} are
     * accessible with a leading {@code /} or relative path.</p>
     *
     * @param vertexResource   classpath path to the vertex shader file
     *                         ( {@code "/shaders/default.vert"}).
     * @param fragmentResource classpath path to the fragment shader file.
     * @return a ready-to-use Shader instance.
     * @throws RuntimeException if a resource cannot be found or read.
     */
    public static Shader fromResource(String vertexResource, String fragmentResource) {
        String vertSrc = readResource(vertexResource);
        String fragSrc = readResource(fragmentResource);
        return new Shader(vertSrc, fragSrc);
    }

    // Binding
    /**
     * Activates this shader program for subsequent draw calls and
     * uniform uploads.
     *
     * <p>Must be called before any {@code upload*} method. OpenGL
     * requires a program to be active ({@code glUseProgram}) before
     * {@code glUniform*} calls will target it.</p>
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Deactivates any shader program.
     */
    public void unbind() {
        glUseProgram(0);
    }

    // Uniform uploads

    /** Uploads a 4x4 matrix uniform. */
    public void uploadMat4(String name, Matrix4f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            mat.get(fb);
            glUniformMatrix4fv(getUniformLocation(name), false, fb);
        }
    }

    /** Uploads a vec4 uniform. */
    public void uploadVec4(String name, Vector4f vec) {
        glUniform4f(getUniformLocation(name), vec.x, vec.y, vec.z, vec.w);
    }

    /** Uploads a vec3 uniform. */
    public void uploadVec3(String name, Vector3f vec) {
        glUniform3f(getUniformLocation(name), vec.x, vec.y, vec.z);
    }

    /** Uploads a vec2 uniform. */
    public void uploadVec2(String name, Vector2f vec) {
        glUniform2f(getUniformLocation(name), vec.x, vec.y);
    }

    /** Uploads a float uniform. */
    public void uploadFloat(String name, float val) {
        glUniform1f(getUniformLocation(name), val);
    }

    /** Uploads an int uniform. */
    public void uploadInt(String name, int val) {
        glUniform1i(getUniformLocation(name), val);
    }

    /**
     * Uploads a texture sampler uniform.
     *
     * <p>This is equivalent to {@code uploadInt} but clarifies intent:
     * the value is the texture unit index (0 for GL_TEXTURE0, etc.).</p>
     *
     * @param name the sampler uniform name in the shader.
     * @param slot the texture unit slot (0-15).
     */
    public void uploadTexture(String name, int slot) {
        glUniform1i(getUniformLocation(name), slot);
    }

    // Cleanup

    /**
     * Deletes the shader program from the GPU.
     *
     * <p>Call this when the shader is no longer needed. After disposal,
     * no further method calls on this instance are valid.</p>
     */
    public void dispose() {
        glDeleteProgram(programId);
    }

    /** @return the OpenGL program ID. */
    public int getProgramId() {
        return programId;
    }

    // Internal helpers
    private int getUniformLocation(String name) {
        return uniformLocationCache.computeIfAbsent(name,
                n -> glGetUniformLocation(programId, n));
    }

    private static int compileStage(int type, String source) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            String stageName = (type == GL_VERTEX_SHADER) ? "vertex" : "fragment";
            glDeleteShader(shaderId);
            throw new RuntimeException(stageName + " shader compilation failed:\n" + log);
        }
        return shaderId;
    }

    private static String readResource(String path) {
        try (InputStream is = Shader.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }
}
