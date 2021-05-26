package com.example.fantasmo_android.fantasmo

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.fantasmo_android.R
import com.example.fantasmo_android.common.helpers.DisplayRotationHelper
import com.example.fantasmo_android.common.helpers.TrackingStateHelper
import com.example.fantasmo_android.common.samplerender.SampleRender
import com.example.fantasmo_android.common.samplerender.arcore.BackgroundRenderer
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class CameraFragment : Fragment(), SampleRender.Renderer{

    private val TAG = "CameraFragment"

    private lateinit var arSession: Session
    private lateinit var currentView: View

    private lateinit var filterRejectionTv: TextView
    private lateinit var anchorDeltaTv: TextView
    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var serverCoordinatesTv: TextView
    private lateinit var trackingFailureTv: TextView
    private lateinit var checkParkingButton: Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    private lateinit var fmLocationManager: FMLocationManager

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private var surfaceView: GLSurfaceView? = null

    private var displayRotationHelper: DisplayRotationHelper? = null
    private lateinit var trackingStateHelper: TrackingStateHelper

    private var render: SampleRender? = null

    private var backgroundRenderer: BackgroundRenderer? = null
    private var hasSetTextureNames = false

    private var anchorIsChecked = false
    private var anchored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.camera_fragment, container, false)

        trackingStateHelper = TrackingStateHelper(requireActivity())
        surfaceView = currentView.findViewById(R.id.surfaceview)
        displayRotationHelper = DisplayRotationHelper( /*context=*/context)

        // Set up renderer.
        render = SampleRender(surfaceView, this, context?.assets)

        filterRejectionTv = currentView.findViewById(R.id.filterRejectionText)
        anchorDeltaTv = currentView.findViewById(R.id.anchorDeltaText)
        cameraTranslationTv = currentView.findViewById(R.id.cameraTranslation)
        cameraAnglesTv = currentView.findViewById(R.id.cameraAnglesText)
        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)
        serverCoordinatesTv = currentView.findViewById(R.id.serverCoordsText)
        trackingFailureTv = currentView.findViewById(R.id.trackingFailureText)
        localizeToggleButton = currentView.findViewById(R.id.localizeToggle)
        anchorToggleButton = currentView.findViewById(R.id.anchorToggle)

        fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!

        return currentView
    }

    override fun onResume() {
        super.onResume()
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            // Create the session.
            arSession = Session(this.context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            configureARSession()
            arSession.resume()
        } catch (e: CameraNotAvailableException) {
            return
        }
        surfaceView!!.onResume()
        displayRotationHelper!!.onResume()

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        //fmLocationManager.isSimulation = true

        // Connect the FMLocationManager from Fantasmo SDK
        fmLocationManager.connect(
            "API_KEY",
            fmLocationListener
        )

        checkParkingButton.setOnClickListener {
            Log.d(TAG, "CheckPark Pressed")
            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 10) {
                Toast.makeText(
                    activity?.applicationContext,
                    "Is Zone In Radius Response: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "LocalizeToggle Enabled")
                // Start getting location updates
                fmLocationManager.startUpdatingLocation()
                filterRejectionTv.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "LocalizeToggle Disabled")
                // Stop getting location updates
                fmLocationManager.stopUpdatingLocation()
                filterRejectionTv.visibility = View.GONE
            }
        }

        anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
            anchorIsChecked = isChecked
            if(!isChecked) {
                Log.d(TAG, "AnchorToggle Disabled")
                anchorDeltaTv.visibility = View.GONE
                fmLocationManager.unsetAnchor()
                anchored = false
            }
        }
    }

    /**
     * Method to configure the AR Session, used to
     * enable auto focus for ARSceneView.
     */
    private fun configureARSession() {
        val config = arSession.config
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED

        config.augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.augmentedImageDatabase = null
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
        config.depthMode = Config.DepthMode.DISABLED

        arSession.configure(config)
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                Log.d(TAG, error.message.toString())
                activity?.runOnUiThread { serverCoordinatesTv.text = error.message.toString() }
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(location: Location, zones: List<FMZone>?) {
                Log.d(TAG, location.toString())
                activity?.runOnUiThread { serverCoordinatesTv.text =
                    "Server Lat: ${location.coordinate.latitude}, Long: ${location.coordinate.longitude}" }
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
                Log.d(TAG, "FrameFilterResult " + didRequestBehavior.displayName)
                activity?.runOnUiThread { filterRejectionTv.text = "FrameFilterResult: ${didRequestBehavior.displayName}" }
            }
        }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     * */
    private fun onUpdate(frame: Frame) {
        val cameraTranslation = frame.androidSensorPose?.translation
        cameraTranslationTv.text = createStringDisplay("Camera Translation: ", cameraTranslation)

        val cameraRotation = frame.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay("Camera Angles: ", cameraRotation)

        val anchorDelta = frame.let { frame2 ->
            fmLocationManager.anchorFrame?.let { anchorFrame ->
                FMUtility.anchorDeltaPoseForFrame(
                    frame2,
                    anchorFrame
                )
            }
        }

        if (anchorDeltaTv.isVisible && anchorDelta != null) {
            val position =
                floatArrayOf(anchorDelta.position.x, anchorDelta.position.y, anchorDelta.position.z)
            anchorDeltaTv.text = createStringDisplay("Anchor Delta: ", position)
        }

        // Show the TrackingFailureReason if the Tracking stops
        if (frame.camera?.trackingState != TrackingState.TRACKING) {
            val errorText = "Tracking error: ${frame.camera?.trackingFailureReason.toString()}"
            trackingFailureTv.text = errorText
            trackingFailureTv.visibility = View.VISIBLE
        } else {
            trackingFailureTv.visibility = View.GONE
        }

        // Localize current frame if not already localizing
        if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
            frame.let { fmLocationManager.localize(it) }
        }

    }

    /**
     * Method to simplify task of creating a String to be shown in the screen
     * */
    private fun createStringDisplay(s: String, cameraAttr: FloatArray?): String {
        return s + String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " +
                String.format("%.2f", cameraAttr?.get(2))
    }

    /**
     * Call onPause method to pause the AR session
     * */
    override fun onPause() {
        super.onPause()
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        displayRotationHelper!!.onPause()
        surfaceView!!.onPause()
        arSession.pause()
    }

    /**
     * Release heap allocation of the AR session
     * */
    override fun onDestroy() {
        super.onDestroy()
        arSession.close()
    }

    /**
     * GL SurfaceView Methods
     * */
    override fun onSurfaceCreated(render: SampleRender?) {
        backgroundRenderer = BackgroundRenderer()
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(render: SampleRender?) {
        if (!hasSetTextureNames) {
            arSession.setCameraTextureNames(intArrayOf(backgroundRenderer!!.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        displayRotationHelper!!.updateSessionIfNeeded(arSession)

        val frame: Frame = try {
            arSession.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            return
        }
        //Acquire ARCore Frame to set anchor and updates UI setting values in the view
        activity?.runOnUiThread {
            onUpdate(frame)
            anchorFrame(frame)
        }

        val camera = frame.camera

        try {
            backgroundRenderer!!.setUseDepthVisualization(false)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            return
        }
        backgroundRenderer!!.updateDisplayGeometry(frame)

        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
        if (frame.timestamp != 0L) {
            backgroundRenderer!!.drawBackground(render)
        }
    }

    private fun anchorFrame(currentArFrame: Frame) {
        if(!anchored){
            if (anchorIsChecked) {
                Log.d(TAG, "AnchorToggle Enabled")

                currentArFrame.let {
                    if (currentArFrame.camera.trackingState == TrackingState.TRACKING) {
                        anchorDeltaTv.visibility = View.VISIBLE
                        fmLocationManager.setAnchor(it)
                    } else {
                        Toast.makeText(
                            activity?.applicationContext,
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
}