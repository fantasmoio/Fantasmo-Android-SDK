package com.fantasmo.sdk.views.common.samplerender;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;

/** A GPU-side texture. */
public class Texture implements Closeable {
    private static final String TAG = Texture.class.getSimpleName();

    private final int[] textureId = {0};
    private final Target target;

    /**
     * Describes the way the texture's edges are rendered.
     *
     * @see <a
     *     href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glTexParameter.xhtml">GL_TEXTURE_WRAP_S</a>.
     */
    public enum WrapMode {
        CLAMP_TO_EDGE(GLES30.GL_CLAMP_TO_EDGE);

        /* package-private */
        final int glesEnum;

        WrapMode(int glesEnum) {
            this.glesEnum = glesEnum;
        }
    }

    /**
     * Describes the target this texture is bound to.
     *
     * @see <a
     *     href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBindTexture.xhtml">glBindTexture</a>.
     */
    public enum Target {
        TEXTURE_2D(GLES30.GL_TEXTURE_2D),
        TEXTURE_EXTERNAL_OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);


        final int glesEnum;

        Target(int glesEnum) {
            this.glesEnum = glesEnum;
        }
    }

    public Texture(Target target, WrapMode wrapMode, boolean useMipmaps) {
        this.target = target;

        GLES30.glGenTextures(1, textureId, 0);
        GLError.maybeThrowGLException("Texture creation failed", "glGenTextures");

        int minFilter = useMipmaps ? GLES30.GL_LINEAR_MIPMAP_LINEAR : GLES30.GL_LINEAR;

        try {
            GLES30.glBindTexture(target.glesEnum, textureId[0]);
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");

            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_S, wrapMode.glesEnum);
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_T, wrapMode.glesEnum);
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    @Override
    public void close() {
        if (textureId[0] != 0) {
            GLES30.glDeleteTextures(1, textureId, 0);
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free texture", "glDeleteTextures");
            textureId[0] = 0;
        }
    }

    /** Retrieve the native texture ID. */
    public int getTextureId() {
        return textureId[0];
    }

    /* package-private */
    Target getTarget() {
        return target;
    }

}
