package com.fantasmo.sdk.views.common.samplerender;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLException;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Represents a GPU shader, the state of its associated uniforms, and some additional draw state.
 */
public class Shader implements Closeable {
    private static final String TAG = Shader.class.getSimpleName();

    /**
     * A factor to be used in a blend function.
     *
     * @see <a
     *     href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFunc.xhtml">glBlendFunc</a>
     */
    public enum BlendFactor {
        ZERO(GLES30.GL_ZERO),
        ONE(GLES30.GL_ONE);

        /* package-private */
        final int glesEnum;

        BlendFactor(int glesEnum) {
            this.glesEnum = glesEnum;
        }
    }

    private int programId = 0;
    private final Map<Integer, Uniform> uniforms = new HashMap<>();
    private int maxTextureUnit = 0;

    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private final Map<Integer, String> uniformNames = new HashMap<>();

    private boolean depthTest = true;
    private boolean depthWrite = true;
    private final BlendFactor sourceRgbBlend = BlendFactor.ONE;
    private final BlendFactor destRgbBlend = BlendFactor.ZERO;
    private final BlendFactor sourceAlphaBlend = BlendFactor.ONE;
    private final BlendFactor destAlphaBlend = BlendFactor.ZERO;

    /**
     * Constructs a {@link Shader} given the shader code.
     *
     * @param defines A map of shader precompiler symbols to be defined with the given names and
     */
    public Shader(
            String vertexShaderCode,
            String fragmentShaderCode,
            Map<String, String> defines) {
        int vertexShaderId = 0;
        int fragmentShaderId = 0;
        String definesCode = createShaderDefinesCode(defines);
        try {
            vertexShaderId =
                    createShader(
                            GLES30.GL_VERTEX_SHADER, insertShaderDefinesCode(vertexShaderCode, definesCode));
            fragmentShaderId =
                    createShader(
                            GLES30.GL_FRAGMENT_SHADER, insertShaderDefinesCode(fragmentShaderCode, definesCode));

            programId = GLES30.glCreateProgram();
            GLError.maybeThrowGLException("Shader program creation failed", "glCreateProgram");
            GLES30.glAttachShader(programId, vertexShaderId);
            GLError.maybeThrowGLException("Failed to attach vertex shader", "glAttachShader");
            GLES30.glAttachShader(programId, fragmentShaderId);
            GLError.maybeThrowGLException("Failed to attach fragment shader", "glAttachShader");
            GLES30.glLinkProgram(programId);
            GLError.maybeThrowGLException("Failed to link shader program", "glLinkProgram");

            final int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == GLES30.GL_FALSE) {
                String infoLog = GLES30.glGetProgramInfoLog(programId);
                GLError.maybeLogGLError(
                        Log.WARN, TAG, "Failed to retrieve shader program info log", "glGetProgramInfoLog");
                throw new GLException(0, "Shader link failed: " + infoLog);
            }
        } catch (Throwable t) {
            close();
            throw t;
        } finally {
            // Shader objects can be flagged for deletion immediately after program creation.
            if (vertexShaderId != 0) {
                GLES30.glDeleteShader(vertexShaderId);
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free vertex shader", "glDeleteShader");
            }
            if (fragmentShaderId != 0) {
                GLES30.glDeleteShader(fragmentShaderId);
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free fragment shader", "glDeleteShader");
            }
        }
    }

    /**
     * Creates a {@link Shader} from the given asset file names.
     *
     * <p>The file contents are interpreted as UTF-8 text.
     *
     * @param defines A map of shader precompiler symbols to be defined with the given names and
     */
    public static Shader createFromAssets(
            String vertexShaderFileName,
            String fragmentShaderFileName,
            Map<String, String> defines)
            throws IOException {
        AssetManager assets = SampleRender.getAssets();
        return new Shader(
                inputStreamToString(assets.open(vertexShaderFileName)),
                inputStreamToString(assets.open(fragmentShaderFileName)),
                defines);
    }

    @Override
    public void close() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
    }

    /**
     * Sets depth test state.
     *
     * @see <a
     *     href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml">glEnable(GL_DEPTH_TEST)</a>.
     */
    public Shader setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
        return this;
    }

    /**
     * Sets depth write state.
     *
     * @see <a
     *     href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDepthMask.xhtml">glDepthMask</a>.
     */
    public Shader setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
        return this;
    }

    /** Sets a texture uniform. */
    public Shader setTexture(String name, Texture texture) {
        // Special handling for Textures. If replacing an existing texture uniform, reuse the texture
        // unit.
        int location = getUniformLocation(name);
        Uniform uniform = uniforms.get(location);
        int textureUnit;
        if (!(uniform instanceof UniformTexture)) {
            textureUnit = maxTextureUnit++;
        } else {
            UniformTexture uniformTexture = (UniformTexture) uniform;
            textureUnit = uniformTexture.getTextureUnit();
        }
        uniforms.put(location, new UniformTexture(textureUnit, texture));
        return this;
    }

    /**
     * Activates the shader. Don't call this directly unless you are doing low level OpenGL code;
     * instead, prefer {@link SampleRender#draw}.
     */
    public void lowLevelUse() {
        // Make active shader/set uniforms
        if (programId == 0) {
            throw new IllegalStateException("Attempted to use freed shader");
        }
        GLES30.glUseProgram(programId);
        GLError.maybeThrowGLException("Failed to use shader program", "glUseProgram");
        GLES30.glBlendFuncSeparate(
                sourceRgbBlend.glesEnum,
                destRgbBlend.glesEnum,
                sourceAlphaBlend.glesEnum,
                destAlphaBlend.glesEnum);
        GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate");
        GLES30.glDepthMask(depthWrite);
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
        if (depthTest) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLError.maybeThrowGLException("Failed to enable depth test", "glEnable");
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            GLError.maybeThrowGLException("Failed to disable depth test", "glDisable");
        }
        try {
            // Remove all non-texture uniforms from the map after setting them, since they're stored as
            // part of the program.
            ArrayList<Integer> obsoleteEntries = new ArrayList<>(uniforms.size());
            for (Map.Entry<Integer, Uniform> entry : uniforms.entrySet()) {
                try {
                    entry.getValue().use(entry.getKey());
                    if (!(entry.getValue() instanceof UniformTexture)) {
                        obsoleteEntries.add(entry.getKey());
                    }
                } catch (GLException e) {
                    String name = uniformNames.get(entry.getKey());
                    throw new IllegalArgumentException("Error setting uniform `" + name + "'", e);
                }
            }
            uniforms.keySet().removeAll(obsoleteEntries);
        } finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to set active texture", "glActiveTexture");
        }
    }

    private interface Uniform {
        void use(int location);
    }

    private static class UniformTexture implements Uniform {
        private final int textureUnit;
        private final Texture texture;

        public UniformTexture(int textureUnit, Texture texture) {
            this.textureUnit = textureUnit;
            this.texture = texture;
        }

        public int getTextureUnit() {
            return textureUnit;
        }

        @Override
        public void use(int location) {
            if (texture.getTextureId() == 0) {
                throw new IllegalStateException("Tried to draw with freed texture");
            }
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit);
            GLError.maybeThrowGLException("Failed to set active texture", "glActiveTexture");
            GLES30.glBindTexture(texture.getTarget().glesEnum, texture.getTextureId());
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
            GLES30.glUniform1i(location, textureUnit);
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");
        }
    }

    private int getUniformLocation(String name) {
        Integer locationObject = uniformLocations.get(name);
        if (locationObject != null) {
            return locationObject;
        }
        int location = GLES30.glGetUniformLocation(programId, name);
        GLError.maybeThrowGLException("Failed to find uniform", "glGetUniformLocation");
        if (location == -1) {
            throw new IllegalArgumentException("Shader uniform does not exist: " + name);
        }
        uniformLocations.put(name, location);
        uniformNames.put(location, name);
        return location;
    }

    private static int createShader(int type, String code) {
        int shaderId = GLES30.glCreateShader(type);
        GLError.maybeThrowGLException("Shader creation failed", "glCreateShader");
        GLES30.glShaderSource(shaderId, code);
        GLError.maybeThrowGLException("Shader source failed", "glShaderSource");
        GLES30.glCompileShader(shaderId);
        GLError.maybeThrowGLException("Shader compilation failed", "glCompileShader");

        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == GLES30.GL_FALSE) {
            String infoLog = GLES30.glGetShaderInfoLog(shaderId);
            GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to retrieve shader info log", "glGetShaderInfoLog");
            GLES30.glDeleteShader(shaderId);
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free shader", "glDeleteShader");
            throw new GLException(0, "Shader compilation failed: " + infoLog);
        }

        return shaderId;
    }

    private static String createShaderDefinesCode(Map<String, String> defines) {
        if (defines == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : defines.entrySet()) {
            builder.append("#define " + entry.getKey() + " " + entry.getValue() + "\n");
        }
        return builder.toString();
    }

    private static String insertShaderDefinesCode(String sourceCode, String definesCode) {
        String result =
                sourceCode.replaceAll(
                        "(?m)^(\\s*#\\s*version\\s+.*)$", "$1\n" + Matcher.quoteReplacement(definesCode));
        if (result.equals(sourceCode)) {
            // No #version specified, so just prepend source
            return definesCode + sourceCode;
        }
        return result;
    }

    private static String inputStreamToString(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream, UTF_8.name());
        char[] buffer = new char[1024 * 4];
        StringBuilder builder = new StringBuilder();
        int amount;
        while ((amount = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, amount);
        }
        reader.close();
        return builder.toString();
    }
}