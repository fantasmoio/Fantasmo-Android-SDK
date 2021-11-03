package com.fantasmo.sdk.views.common.samplerender.arcore;

import com.fantasmo.sdk.views.common.samplerender.Mesh;
import com.fantasmo.sdk.views.common.samplerender.SampleRender;
import com.fantasmo.sdk.views.common.samplerender.Shader;
import com.fantasmo.sdk.views.common.samplerender.Texture;
import com.fantasmo.sdk.views.common.samplerender.VertexBuffer;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * This class both renders the AR camera background and composes the a scene foreground. The camera
 * background can be rendered as either camera image data or camera depth data. The virtual scene
 * can be composited with or without depth occlusion.
 */
public class BackgroundRenderer {

    // components_per_vertex * number_of_vertices * float_size
    private static final int COORDS_BUFFER_SIZE = 2 * 4 * 4;

    private static final FloatBuffer NDC_QUAD_COORDS_BUFFER =
            ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private static final FloatBuffer VIRTUAL_SCENE_TEX_COORDS_BUFFER =
            ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

    static {
        NDC_QUAD_COORDS_BUFFER.put(
                new float[] {
                        /*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f,
                });
        VIRTUAL_SCENE_TEX_COORDS_BUFFER.put(
                new float[] {
                        /*0:*/ 0f, 0f, /*1:*/ 1f, 0f, /*2:*/ 0f, 1f, /*3:*/ 1f, 1f,
                });
    }

    private final FloatBuffer cameraTexCoords =
            ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private final Mesh mesh;
    private final VertexBuffer cameraTexCoordsVertexBuffer;
    private Shader backgroundShader;

    private final Texture cameraColorTexture;
    private final Texture cameraDepthTexture;

    private boolean useDepthVisualization;

    /**
     * Allocates and initializes OpenGL resources needed by the background renderer. Must be called
     * during a {@link SampleRender.Renderer} callback, typically in {@link
     * SampleRender.Renderer#onSurfaceCreated()}.
     */
    public BackgroundRenderer() {
        cameraColorTexture =
                new Texture(
                        Texture.Target.TEXTURE_EXTERNAL_OES,
                        Texture.WrapMode.CLAMP_TO_EDGE,
                        /*useMipmaps=*/ false);
        cameraDepthTexture =
                new Texture(
                        Texture.Target.TEXTURE_2D,
                        Texture.WrapMode.CLAMP_TO_EDGE,
                        /*useMipmaps=*/ false);

        // Create a Mesh with three vertex buffers: one for the screen coordinates (normalized device
        // coordinates), one for the camera texture coordinates (to be populated with proper data later
        // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
        VertexBuffer screenCoordsVertexBuffer =
                new VertexBuffer( /* numberOfEntriesPerVertex=*/ 2, NDC_QUAD_COORDS_BUFFER);
        cameraTexCoordsVertexBuffer =
                new VertexBuffer( /*numberOfEntriesPerVertex=*/ 2, /*entries=*/ null);
        VertexBuffer virtualSceneTexCoordsVertexBuffer =
                new VertexBuffer( /* numberOfEntriesPerVertex=*/ 2, VIRTUAL_SCENE_TEX_COORDS_BUFFER);
        VertexBuffer[] vertexBuffers = {
                screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer, virtualSceneTexCoordsVertexBuffer,
        };
        mesh =
                new Mesh(Mesh.PrimitiveMode.TRIANGLE_STRIP, /*indexBuffer=*/ null, vertexBuffers);
    }

    /**
     * Sets whether the background camera image should be replaced with a depth visualization instead.
     * This reloads the corresponding shader code, and must be called on the GL thread.
     */
    public void setUseDepthVisualization(boolean useDepthVisualization)
            throws IOException {
        if (backgroundShader != null) {
            if (this.useDepthVisualization == useDepthVisualization) {
                return;
            }
            backgroundShader.close();
            backgroundShader = null;
            this.useDepthVisualization = useDepthVisualization;
        }
        if (useDepthVisualization) {
            backgroundShader =
                    Shader.createFromAssets(
                            "shaders/background_show_depth_color_visualization.vert",
                            "shaders/background_show_depth_color_visualization.frag",
                            /*defines=*/ null)
                            .setTexture("u_CameraDepthTexture", cameraDepthTexture)
                            .setDepthTest(false)
                            .setDepthWrite(false);
        } else {
            backgroundShader =
                    Shader.createFromAssets(
                            "shaders/background_show_camera.vert",
                            "shaders/background_show_camera.frag",
                            /*defines=*/ null)
                            .setTexture("u_CameraColorTexture", cameraColorTexture)
                            .setDepthTest(false)
                            .setDepthWrite(false);
        }

    }

    /**
     * Updates the display geometry. This must be called every frame before calling either of
     * BackgroundRenderer's draw methods.
     *
     * @param frame The current {@code Frame} as returned by {@link Session#update()}.
     */
    public void updateDisplayGeometry(Frame frame) {
        if (frame.hasDisplayGeometryChanged()) {
            // If display rotation changed (also includes view size change), we need to re-query the UV
            // coordinates for the screen rect, as they may have changed as well.
            frame.transformCoordinates2d(
                    Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                    NDC_QUAD_COORDS_BUFFER,
                    Coordinates2d.TEXTURE_NORMALIZED,
                    cameraTexCoords);
            cameraTexCoordsVertexBuffer.set(cameraTexCoords);
        }
    }

    /**
     * Draws the AR background image. The image will be drawn such that virtual content rendered with
     * the matrices provided by {@link com.google.ar.core.Camera#getViewMatrix(float[], int)} and
     * {@link com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)} will
     * accurately follow static physical objects.
     */
    public void drawBackground(SampleRender render) {
        render.draw(mesh, backgroundShader);
    }

    /** Return the camera color texture generated by this object. */
    public Texture getCameraColorTexture() {
        return cameraColorTexture;
    }

}
