package com.example.fantasmo_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.Size
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
import com.fantasmo.sdk.*
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapView
import com.google.ar.core.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment() {

    private val TAG = "ARCoreFragment"

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

    private lateinit var mapButton: Button
    private lateinit var googleMapView: MapView
    private lateinit var googleMapsManager: GoogleMapsManager

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

    private lateinit var qrReader: QRCodeReader
    private lateinit var urlView: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var qrEnabler: Switch
    private var qrReaderEnabled = false

    private var behaviorReceived = 0L
    private var n2s = 1_000_000_000L
    private val behaviorThreshold = 1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

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

        googleMapView = currentView.findViewById(R.id.mapView)
        mapButton = currentView.findViewById(R.id.mapButton)
        googleMapsManager = GoogleMapsManager(requireActivity(), googleMapView)
        googleMapsManager.initGoogleMap(savedInstanceState)

        qrEnabler = currentView.findViewById(R.id.qrEnabler)
        urlView = currentView.findViewById(R.id.qrResultView)
        qrReader = QRCodeReader(urlView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            Log.e(TAG, "Your GPS seems to be disabled")
        }
        return currentView
    }

    /**
     * To make any changes or get any values from ArFragment always call onResume lifecycle
     * */
    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
        val appSessionId = UUID.randomUUID().toString()

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

                fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 10) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "Is Zone In Radius Response: $it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            mapButton.setOnClickListener {
                if (googleMapView.visibility == View.VISIBLE) {
                    googleMapView.visibility = View.GONE
                } else {
                    googleMapView.visibility = View.VISIBLE
                }
            }

            localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d(TAG, "LocalizeToggle Enabled")

                    // Start getting location updates
                    fmLocationManager.startUpdatingLocation(appSessionId, true)
                    filterRejectionTv.visibility = View.VISIBLE
                } else {
                    Log.d(TAG, "LocalizeToggle Disabled")

                    // Stop getting location updates
                    fmLocationManager.stopUpdatingLocation()
                    filterRejectionTv.visibility = View.GONE
                }
            }

            anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
                googleMapsManager.updateAnchor(isChecked)
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
                    googleMapsManager.unsetAnchor()
                }
            }

            qrEnabler.setOnCheckedChangeListener { _, isChecked ->
                qrReaderEnabled = if (isChecked) {
                    Log.d(TAG, "QR Reader Enabled")
                    true
                } else {
                    Log.d(TAG, "QR Reader Disabled")
                    false
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

        var selectedSize = Size(0, 0)
        var selectedCameraConfig = 0

        val filter = CameraConfigFilter(arSession)
        val cameraConfigsList: List<CameraConfig> = arSession.getSupportedCameraConfigs(filter)
        for (currentCameraConfig in cameraConfigsList) {
            val cpuImageSize: Size = currentCameraConfig.imageSize
            val gpuTextureSize: Size = currentCameraConfig.textureSize
            Log.d(
                TAG,
                "CurrentCameraConfig CPU image size:$cpuImageSize GPU texture size:$gpuTextureSize"
            )
            if (cpuImageSize.width > selectedSize.width && cpuImageSize.height <= 1080) {
                selectedSize = cpuImageSize
                selectedCameraConfig = cameraConfigsList.indexOf(currentCameraConfig)
            }
        }
        arSession.cameraConfig = cameraConfigsList[selectedCameraConfig]
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
            override fun locationManager(result: FMLocationResult) {
                Log.d(TAG, result.confidence.toString())
                Log.d(TAG, result.location.toString())
                activity?.runOnUiThread {
                    serverCoordinatesTv.text =
                        "Server Lat: ${result.location.coordinate.latitude}, Long: ${result.location.coordinate.longitude}"
                    googleMapsManager.addCorrespondingMarkersToMap(
                        result.location.coordinate.latitude,
                        result.location.coordinate.longitude
                    )
                }
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
                behaviorReceived = System.nanoTime()
                Log.d(TAG, "FrameFilterResult " + didRequestBehavior.displayName)
                activity?.runOnUiThread {
                    filterRejectionTv.text = "FrameFilterResult: ${didRequestBehavior.displayName}"
                }
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

        // Only read frame if the qrCodeReader is enabled and only if qrCodeReader is in reading mode
        if (qrReaderEnabled && !qrReader.qrReading) {
            arFrame?.let { qrReader.processImage(it) }
        }

        // After a second, clear behavior message displayed
        val currentTime = System.nanoTime()
        if ((currentTime - behaviorReceived) / n2s > behaviorThreshold) {
            val clearText = "FrameFilterResult"
            filterRejectionTv.text = clearText
        }
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
                Looper.myLooper()!!
            )
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
     * Release heap allocation of the AR session
     * */
    override fun onDestroy() {
        arSession.close()
        googleMapView.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        googleMapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    override fun onPause() {
        googleMapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }
}