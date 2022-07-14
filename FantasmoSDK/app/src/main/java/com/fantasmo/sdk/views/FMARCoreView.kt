package com.fantasmo.sdk.views

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMPose
import com.fantasmo.sdk.views.common.helpers.DisplayRotationHelper
import com.fantasmo.sdk.views.common.helpers.TrackingStateHelper
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import com.fantasmo.sdk.views.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Class responsible by the ARCore management and keeping it on throughout the app lifecycles.
 */
internal class FMARCoreView(
    private val arLayout: CoordinatorLayout,
    val context: Context
) :
    SampleRender.Renderer {

    var connected: Boolean = false
    private val TAG = FMARCoreView::class.java.simpleName

    private var arSession: Session? = null
    lateinit var arSessionListener: FMARSessionListener

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
    private var lastArFrameTimestamp: Long = 0L

    private var coroutineScope = CoroutineScope(Dispatchers.Default)

    // Set anchor after QR code is read
    private var anchorIsChecked = false
    // Result of anchoring
    private var anchored = false

    fun setupARSession() {
        surfaceView = arLayout.findViewWithTag("SurfaceView")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
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
                return
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            arSession!!.pause()
            configureARSession()
            // To record a live camera session for later playback, call
            // `session.startRecording(recorderConfig)` at anytime. To playback a previously recorded AR
            // session instead of using the live camera feed, call
            // `session.setPlaybackDataset(playbackDatasetPath)` before calling `session.resume()`. To
            // learn more about recording and playback, see:
            // https://developers.google.com/ar/develop/java/recording-and-playback
            arSession!!.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG,"Camera not available. Try restarting the app.")
            return
        } catch(e: Exception) {
            e.localizedMessage?.let { Log.e(TAG, it) }
            return
        }
        surfaceView.onResume()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            displayRotationHelper.onResume()
    }

    /**
     * Pause AR Session
     */
    fun onPause() {
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        if (arSession != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
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
        config.instantPlacementMode =  Config.InstantPlacementMode.DISABLED
        config.augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        config.depthMode = Config.DepthMode.DISABLED
        config.augmentedImageDatabase = AugmentedImageDatabase(arSession!!)

        val filter = CameraConfigFilter(arSession)
        val cameraConfigsList: List<CameraConfig> = arSession!!.getSupportedCameraConfigs(filter)
        var selectedCameraConfig = 0

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var selectedSize = Size(0, 0)

            for (currentCameraConfig in cameraConfigsList) {
                val cpuImageSize: Size = currentCameraConfig.imageSize
                if (cpuImageSize.width > selectedSize.width && cpuImageSize.height <= 1080) {
                    selectedSize = cpuImageSize
                    selectedCameraConfig = cameraConfigsList.indexOf(currentCameraConfig)
                }
            }
            Log.i(
                TAG,
                "CurrentCameraConfig CPU image size:$selectedSize"
            )
            arSession!!.cameraConfig = cameraConfigsList[selectedCameraConfig]
        }
        arSession!!.configure(config)
    }

    /**
     * GL SurfaceView Methods
     */
    override fun onSurfaceCreated(render: SampleRender?) {
        backgroundRenderer = BackgroundRenderer()
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            displayRotationHelper.updateSessionIfNeeded(arSession!!)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera frame rate.
        val frame: Frame = try {
            arSession!!.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            return
        }
        val fmFrame = FMFrame(frame, context)
        val newArFrameTimestamp = fmFrame.timestamp
        //Acquire ARCore Frame to set anchor and updates UI setting values in the view
        coroutineScope.launch {
            // Code here will run in Default thread
            if(connected && newArFrameTimestamp > lastArFrameTimestamp){
                onUpdate(fmFrame)
                lastArFrameTimestamp = newArFrameTimestamp
            }
        }

        val camera = fmFrame.camera

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
        if (fmFrame.timestamp != 0L) {
            backgroundRenderer.drawBackground(render)
        }
    }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Also responsible for frame anchoring and qrScanning with arFrames
     */
    private fun onUpdate(fmFrame: FMFrame) {

        arSessionListener.localize(fmFrame)

        if (!anchored) {
            if (anchorIsChecked) {
                anchored = arSessionListener.anchored(fmFrame)
            }
        }

        arSessionListener.qrCodeScan(fmFrame)
    }

    fun startAnchor() {
        anchorIsChecked = true
        anchored = false
    }

    fun isAnchored(): Boolean {
        return anchored
    }

    fun unsetAnchor() {
        anchored = true
    }
}

/**
 * Listener designed to keep encapsulation between the ARCoreView
 * and other classes that need values from the AR session.
 */
internal interface FMARSessionListener{
    /**
     * When the SDK enters the localization session, this provides the frame
     * to localize and passes to the `FMLocationManager.session()` method.
     */
    fun localize(fmFrame: FMFrame)

    /**
     * Sends the state of the anchor. In case of success returns `true`,
     * otherwise returns `false` telling the user to try again.
     */
    fun anchored(fmFrame: FMFrame): Boolean

    /**
     * Gets the FMPose regarding the difference between the anchor and current frame.
     */
    fun anchorDelta(fmFrame: FMFrame): FMPose?

    /**
     * Sends a frame to the QRCodeReader and extract a QR Code from it.
     */
    fun qrCodeScan(fmFrame: FMFrame)
}