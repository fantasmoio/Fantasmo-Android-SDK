package com.fantasmo.sdk.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.QRCodeScanner
import com.fantasmo.sdk.views.common.helpers.DisplayRotationHelper
import com.fantasmo.sdk.views.common.helpers.TrackingStateHelper
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import com.fantasmo.sdk.views.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException

class FMARCoreView(private val arLayout: CoordinatorLayout, val context: Context) :
    SampleRender.Renderer {

    private val TAG = "FMARCoreManager"

    private var arSession: Session? = null
    lateinit var fmLocationManager: FMLocationManager

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    // This is important because ARCore needs a GL context even when it doesn't need to render
    // to the screen. Also important due to record and playback functionality because they both
    // need to reestablish textures from the camera which is given by background renderer
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var render: SampleRender
    private lateinit var backgroundRenderer: BackgroundRenderer
    private var hasSetTextureNames = false

    private lateinit var displayRotationHelper: DisplayRotationHelper
    private lateinit var trackingStateHelper: TrackingStateHelper

    lateinit var filterRejectionTv: TextView
    private lateinit var anchorDeltaTv: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    var anchorIsChecked = false
    var anchored = false

    private var behaviorReceived = 0L
    private var n2s = 1_000_000_000L
    private val behaviorThreshold = 1L

    lateinit var qrCodeReader: QRCodeScanner
    var localizing = false

    private fun helloWorld() {
        Log.d(TAG, "Setting ARCore Session")
    }

    fun setupARSession() {
        helloWorld()

        surfaceView = arLayout.findViewWithTag("SurfaceView")
        displayRotationHelper = DisplayRotationHelper(context)
        trackingStateHelper = TrackingStateHelper(context as Activity?)

        // Set up renderer.
        render = SampleRender(surfaceView, this, context.assets)
        onResume()
    }

    /**
     * Android Lifecycle events
     */
    fun onResume() {
        if (arSession == null) {
            try {
                // Create the session.
                arSession = Session(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureARSession()
            // To record a live camera session for later playback, call
            // `session.startRecording(recorderConfig)` at anytime. To playback a previously recorded AR
            // session instead of using the live camera feed, call
            // `session.setPlaybackDataset(playbackDatasetPath)` before calling `session.resume()`. To
            // learn more about recording and playback, see:
            // https://developers.google.com/ar/develop/java/recording-and-playback
            arSession!!.resume()
        } catch (e: CameraNotAvailableException) {
            return
        }
        surfaceView.onResume()
        displayRotationHelper.onResume()
    }

    /**
     * Pause AR Session
     */
    fun onPause() {
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        if (arSession == null) {
            displayRotationHelper.onPause()
            surfaceView.onPause()
            arSession!!.pause()
        }
    }

    /**
     * Release heap allocation of the AR session
     */
    fun onDestroy() {
        // Explicitly close ARCore Session to release native resources.
        // Review the API reference for important considerations before calling close() in apps with
        // more complicated lifecycle requirements:
        // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
        if (arSession != null) {
            arSession!!.close()
            arSession = null
        }
    }

    /**
     * Method to configure the AR Session, used to
     * enable auto focus for ARSceneView.
     */
    private fun configureARSession() {
        val config = arSession!!.config
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED

        var selectedSize = Size(0, 0)
        var selectedCameraConfig = 0

        val filter = CameraConfigFilter(arSession)
        val cameraConfigsList: List<CameraConfig> = arSession!!.getSupportedCameraConfigs(filter)
        for (currentCameraConfig in cameraConfigsList) {
            val cpuImageSize: Size = currentCameraConfig.imageSize
            val gpuTextureSize: Size = currentCameraConfig.textureSize
            Log.d(
                TAG,
                "Available CameraConfigs: CPU image size:$cpuImageSize GPU texture size:$gpuTextureSize"
            )
            if (cpuImageSize.width > selectedSize.width && cpuImageSize.height <= 1080) {
                selectedSize = cpuImageSize
                selectedCameraConfig = cameraConfigsList.indexOf(currentCameraConfig)
            }
        }
        Log.d(
            TAG,
            "CurrentCameraConfig CPU image size:$selectedSize"
        )
        arSession!!.cameraConfig = cameraConfigsList[selectedCameraConfig]
        arSession!!.configure(config)
    }

    /**
     * GL SurfaceView Methods
     */
    override fun onSurfaceCreated(render: SampleRender?) {
        backgroundRenderer = BackgroundRenderer()
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(render: SampleRender?) {
        if (arSession == null) {
            return
        }
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            arSession!!.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(arSession)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame: Frame = try {
            arSession!!.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            return
        }
        //Acquire ARCore Frame to set anchor and updates UI setting values in the view
        (context as Activity).runOnUiThread {
            // Code here will run in UI thread
            onUpdate(frame)
            anchorFrame(frame)
            qrScanFrame(frame)
        }

        val camera = frame.camera

        // Update BackgroundRenderer state to match the depth settings. False for this case
        try {
            backgroundRenderer.setUseDepthVisualization(false)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            return
        }
        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
        if (frame.timestamp != 0L) {
            backgroundRenderer.drawBackground(render)
        }
    }

    private fun qrScanFrame(frame: Frame) {
        // Only read frame if the qrCodeReader is enabled and only if qrCodeReader is in reading mode
        if (qrCodeReader.qrCodeReaderEnabled && qrCodeReader.state == QRCodeScanner.State.IDLE) {
            Log.d(TAG,"QR Code Scanning")
            frame.let { qrCodeReader.processImage(it) }
        }

        if(!qrCodeReader.qrCodeReaderEnabled){
            localizing = true
        }
    }

    /**
     * Frame Anchoring
     */
    private fun anchorFrame(currentArFrame: Frame) {
        if (!anchored) {
            if (anchorIsChecked) {
                Log.d(TAG, "AnchorToggle Enabled")

                currentArFrame.let {
                    if (currentArFrame.camera.trackingState == TrackingState.TRACKING) {
                        //anchorDeltaTv.visibility = View.VISIBLE
                        fmLocationManager.setAnchor(it)
                    } else {
                        Toast.makeText(
                            context.applicationContext,
                            "Anchor can't be set because tracking state is not correct, please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        anchorToggleButton.isChecked = false
                    }
                }
                anchored = true
            }
        }
    }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     * */
    private fun onUpdate(frame: Frame) {
        val anchorDelta = frame.let { frame2 ->
            fmLocationManager.anchorFrame?.let { anchorFrame ->
                FMUtility.anchorDeltaPoseForFrame(
                    frame2,
                    anchorFrame
                )
            }
        }

        if (//anchorDeltaTv.isVisible &&
            anchorDelta != null) {
            val position =
                floatArrayOf(
                    anchorDelta.position.x,
                    anchorDelta.position.y,
                    anchorDelta.position.z
                )
            Log.d(TAG,"Anchor Delta: ${createStringDisplay(position)}")
            //anchorDeltaTv.text = createStringDisplay("Anchor Delta: ", position)
        }

        // Localize current frame if not already localizing
        if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
            fmLocationManager.localize(frame)
        }

        val currentTime = System.nanoTime()
        if ((currentTime - behaviorReceived) / n2s > behaviorThreshold) {
            val clearText = "FrameFilterResult"
            filterRejectionTv.text = clearText
            filterRejectionTv.visibility = View.GONE
        }
    }

    /**
     * Method to simplify task of creating a String to be shown in the screen
     * */
    private fun createStringDisplay(cameraAttr: FloatArray?): String {
        return String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " +
                String.format("%.2f", cameraAttr?.get(2))
    }
}