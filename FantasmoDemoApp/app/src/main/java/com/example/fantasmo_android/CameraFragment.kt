package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.validators.FMBehaviorRequest
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class CameraFragment : Fragment() {

    private val TAG = "CameraFragment"

    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
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

    private lateinit var locationManager: LocationManager
    private lateinit var fmLocationManager: FMLocationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.camera_fragment, container, false)

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
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return currentView
    }

    /**
     * To make any changes or get any values from ArFragment always call onResume lifecycle
     * */
    override fun onStart() {
        super.onStart()

        try {
            arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arSceneView = arFragment.arSceneView

            configureARSession()
            val scene = arSceneView.scene
            scene.addOnUpdateListener { frameTime ->
                run {
                    arFragment.onUpdate(frameTime)
                    onUpdate()
                }
            }

            // Enable simulation mode to test purposes with specific location
            // depending on which SDK flavor it's being used (Paris, Munich, Miami)
            //fmLocationManager.isSimulation = true

            // Connect the FMLocationManager from Fantasmo SDK
            fmLocationManager.connect(
                "8e785284ca284c01bd84116c0d18e8fd",
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
                if (isChecked) {
                    Log.d(TAG, "AnchorToggle Enabled")

                    val currentArFrame = arSceneView.arFrame
                    currentArFrame?.let {
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
                } else {
                    Log.d(TAG, "AnchorToggle Disabled")

                    anchorDeltaTv.visibility = View.GONE
                    fmLocationManager.unsetAnchor()
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, "ArFragment Null")
        }
    }

    /**
     * Method to configure the AR Session, used to
     * enable auto focus for ARSceneView.
     */
    private fun configureARSession() {
        arSession = Session(context)
        val config = Config(arSession)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        arSession.configure(config)
        arSceneView.setupSession(arSession)
        Log.d(TAG, arSceneView.session?.config.toString())
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
                Log.d(TAG, didRequestBehavior.displayName)
                activity?.runOnUiThread { filterRejectionTv.text = "FrameFilterResult: ${didRequestBehavior.displayName}" }
            }
        }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     * */
    private fun onUpdate() {
        val arFrame = arSceneView.arFrame

        val cameraTranslation = arFrame?.androidSensorPose?.translation
        cameraTranslationTv.text = createStringDisplay("Camera Translation: ", cameraTranslation)

        val cameraRotation = arFrame?.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay("Camera Angles: ", cameraRotation)

        val anchorDelta = arFrame?.let { frame ->
            fmLocationManager.anchorFrame?.let { anchorFrame ->
                FMUtility.anchorDeltaPoseForFrame(
                    frame,
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
        if (arFrame?.camera?.trackingState != TrackingState.TRACKING) {
            val errorText = "Tracking error: ${arFrame?.camera?.trackingFailureReason.toString()}"
            trackingFailureTv.text = errorText
            trackingFailureTv.visibility = View.VISIBLE
        } else {
            trackingFailureTv.visibility = View.GONE
        }

        // Localize current frame if not already localizing
        if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
            arFrame?.let { fmLocationManager.localize(it) }
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
}