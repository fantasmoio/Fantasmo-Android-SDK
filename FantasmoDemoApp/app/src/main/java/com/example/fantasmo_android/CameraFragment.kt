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
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.google.ar.core.Config
import com.google.ar.core.Session
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

    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var deviceCoorTv: TextView
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

        cameraTranslationTv = currentView.findViewById(R.id.cameraTranslation)
        cameraAnglesTv = currentView.findViewById(R.id.cameraAnglesText)
        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)
        deviceCoorTv = currentView.findViewById(R.id.coordinatesText)
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
                // Add Check Parking Functionality
                Log.d("CameraFragment-> CheckPark Pressed", "CheckPark")
            }

            localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d("CameraFragment-> LocalizeToggle", "Enabled")

                    // Start getting location updates
                    fmLocationManager.startUpdatingLocation()
                } else {
                    Log.d("CameraFragment-> LocalizeToggle", "Disabled")

                    // Stop getting location updates
                    fmLocationManager.startUpdatingLocation()
                }
            }

            anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d("CameraFragment-> AnchorToggle", "Enabled")
                } else {
                    Log.d("CameraFragment-> AnchorToggle", "Disabled")
                }
            }

        } catch (e: Exception) {
            Log.d("CameraFragment-> ArFragment Null", "ArFragment")
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
        Log.d("CameraFragment-> Session", arSceneView.session?.config.toString())
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
            object : FMLocationListener {
                override fun locationManager(error: ErrorResponse, metadata: Any?) {
                    Log.d(TAG, error.message.toString())
                }

                override fun locationManager(location: Location, zones: List<FMZone>?) {
                    Log.d(TAG, location.toString())
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