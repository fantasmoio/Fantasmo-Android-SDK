package com.example.fantasmo_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.google.android.gms.location.*
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment() {

    private val TAG = "CameraFragment"

    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var arSession: Session
    private lateinit var currentView: View

    private lateinit var anchorDeltaTv: TextView
    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var serverCoorTv: TextView
    private lateinit var checkParkingButton: Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")

    private lateinit var fmLocationManager: FMLocationManager

    private val locationInterval = 300L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        anchorDeltaTv = currentView.findViewById(R.id.anchorDeltaText)
        cameraTranslationTv = currentView.findViewById(R.id.cameraTranslation)
        cameraAnglesTv = currentView.findViewById(R.id.cameraAnglesText)
        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)
        serverCoorTv = currentView.findViewById(R.id.serverCoordsText)
        localizeToggleButton = currentView.findViewById(R.id.localizeToggle)
        anchorToggleButton = currentView.findViewById(R.id.anchorToggle)

        fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            Log.e(TAG, "Your GPS seems to be disabled")
        }
        return currentView
    }

    /**
     * onStart lifecycle establishes listeners for the buttons and SDK calls
     * It's also responsible for creating arFragment and arSession
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
                "API_KEY",
                fmLocationListener
            )

            checkParkingButton.setOnClickListener {
                Log.d(TAG, "CheckPark Pressed")

                fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 23) {
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
                } else {
                    Log.d(TAG, "LocalizeToggle Disabled")

                    // Stop getting location updates
                    fmLocationManager.stopUpdatingLocation()
                }
            }

            anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d(TAG, "AnchorToggle Enabled")

                    anchorDeltaTv.visibility = View.VISIBLE

                    val currentArFrame = arSceneView.arFrame
                    currentArFrame?.let { fmLocationManager.setAnchor(it) }
                } else {
                    Log.d(TAG, "AnchorToggle Disabled")

                    anchorDeltaTv.visibility = View.INVISIBLE

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
                serverCoorTv.text = error.message.toString()
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(location: Location, zones: List<FMZone>?) {
                Log.d(TAG, location.toString())
                serverCoorTv.text =
                    "Server Lat: ${location.coordinate.latitude}, Long: ${location.coordinate.longitude}"
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

        val anchorDelta = arFrame?.let { fmLocationManager.anchorDeltaPoseForFrame(it) }
        if (anchorDeltaTv.isVisible && anchorDelta != null) {
            val position =
                floatArrayOf(anchorDelta.position.x, anchorDelta.position.y, anchorDelta.position.z)
            anchorDeltaTv.text = createStringDisplay("Anchor Delta: ", position)
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

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Sets the [fmLocationManager.currentLocation] coordinates used to localize.
     */
    private fun getLocation() {
        if ((context.let {
                PermissionChecker.checkSelfPermission(
                    it!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) &&
            (context.let {
                PermissionChecker.checkSelfPermission(
                    it!!,
                    Manifest.permission.CAMERA
                )
            } != PackageManager.PERMISSION_GRANTED)) {
            Log.e(TAG, "Location permission needs to be granted.")
        } else {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.smallestDisplacement = 1f
            locationRequest.fastestInterval = locationInterval
            locationRequest.interval = locationInterval

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
                    //Set SDK Location
                    fmLocationManager.setLocation(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                    Log.d(TAG, "onLocationResult: ${locationResult.lastLocation}")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        }
    }
}