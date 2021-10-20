package com.fantasmo.sdk.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.opengl.GLSurfaceView
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.PermissionChecker
import com.fantasmo.sdk.*
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.views.common.helpers.DisplayRotationHelper
import com.fantasmo.sdk.views.common.helpers.TrackingStateHelper
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import com.fantasmo.sdk.views.common.samplerender.arcore.BackgroundRenderer
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapView
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException
import java.util.*

class FMARCoreView(private val arLayout: CoordinatorLayout, val context: Context) :
    SampleRender.Renderer {

    private val TAG = "FMARCoreManager"

    private var arSession: Session? = null
    private lateinit var fmLocationManager: FMLocationManager

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

    private lateinit var filterRejectionTv: TextView
    private lateinit var anchorDeltaTv: TextView
    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var lastResultTv: TextView

    private lateinit var statusTv: TextView
    private lateinit var localizeTv: TextView
    private lateinit var uploadTv: TextView
    private lateinit var distanceTravelledTv: TextView
    private lateinit var cameraAnglesSpreadTv: TextView
    private lateinit var normalTv: TextView
    private lateinit var limitedTv: TextView
    private lateinit var notAvailableTv: TextView
    private lateinit var excessiveMotionTv: TextView
    private lateinit var insufficientFeaturesTv: TextView
    private lateinit var pitchLowTv: TextView
    private lateinit var pitchHighTv: TextView
    private lateinit var blurryTv: TextView
    private lateinit var tooFastTv: TextView
    private lateinit var tooLittleTv: TextView
    private lateinit var featuresTv: TextView

    private lateinit var mapButton: Button

    lateinit var googleMapView: MapView
    lateinit var googleMapsManager: GoogleMapsView

    private lateinit var checkParkingButton: Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var qrToggleButton: Switch

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")

    private val locationInterval = 300L

    private var anchorIsChecked = false
    private var anchored = false

    private var behaviorReceived = 0L
    private var n2s = 1_000_000_000L
    private val behaviorThreshold = 1L

    lateinit var qrCodeReader: QRCodeReader
    private var qrCodeReaderEnabled = false

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

    fun setupFantasmoEnvironment(
        accessToken: String,
        appSessionId: String,
        showStatistics: Boolean,
        isSimulation: Boolean,
        usesInternalLocationManager: Boolean
    ) {
        if (usesInternalLocationManager) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getLocation()
            } else {
                Log.e(TAG, "Your GPS seems to be disabled")
            }
        }
        statusTv = arLayout.findViewById(R.id.statusTextView)
        fmLocationManager = FMLocationManager(context)
        fmLocationManager.isSimulation = isSimulation

        // Connect the FMLocationManager from Fantasmo SDK
        if (fmLocationListener == null) {
            Log.d(TAG, "LocationListener is null")
        } else {
            fmLocationManager.connect(
                accessToken,
                fmLocationListener
            )
        }

        val statistics = arLayout.findViewWithTag<ConstraintLayout>("StatisticsView")
        if (showStatistics) {
            statistics.visibility = View.VISIBLE
        } else {
            statistics.visibility = View.GONE
        }

        //anchorDeltaTv = arLayout.findViewById(R.id.anchorDeltaText)
        cameraTranslationTv = arLayout.findViewById(R.id.translationTextView)
        cameraAnglesTv = arLayout.findViewById(R.id.cameraAnglesTextView)
        lastResultTv = arLayout.findViewById(R.id.lastResultTextView)

        localizeTv = arLayout.findViewById(R.id.localizeTimeTextView)
        uploadTv = arLayout.findViewById(R.id.uploadTimeTextView)
        distanceTravelledTv = arLayout.findViewById(R.id.distanceTravelledTextView)
        cameraAnglesSpreadTv = arLayout.findViewById(R.id.cameraAnglesSpreadTextView)
        normalTv = arLayout.findViewById(R.id.normalTextView)
        limitedTv = arLayout.findViewById(R.id.limitedTextView)
        notAvailableTv = arLayout.findViewById(R.id.notAvailableTextView)
        excessiveMotionTv = arLayout.findViewById(R.id.excessiveMotionTextView)
        insufficientFeaturesTv = arLayout.findViewById(R.id.insufficientFeaturesTextView)
        pitchLowTv = arLayout.findViewById(R.id.pitchLowTextView)
        pitchHighTv = arLayout.findViewById(R.id.pitchHighTextView)
        blurryTv = arLayout.findViewById(R.id.blurryTextView)
        tooFastTv = arLayout.findViewById(R.id.tooFastTextView)
        tooLittleTv = arLayout.findViewById(R.id.tooLittleTextView)
        featuresTv = arLayout.findViewById(R.id.featuresTextView)

        filterRejectionTv = arLayout.findViewById(R.id.filterRejectionTextView)
        localizeToggleButton = arLayout.findViewById(R.id.localizeToggle)
        localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "LocalizeToggle Enabled")

                // Start getting location updates
                fmLocationManager.startUpdatingLocation(appSessionId, true)
            } else {
                Log.d(TAG, "LocalizeToggle Disabled")

                // Stop getting location updates
                fmLocationManager.stopUpdatingLocation()
            }
        }

        checkParkingButton = arLayout.findViewById(R.id.checkParkingButton)
        checkParkingButton.setOnClickListener {
            Log.d(TAG, "CheckPark Pressed")

            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 10) {
                Toast.makeText(
                    context.applicationContext,
                    "Is Zone In Radius Response: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        googleMapView = arLayout.findViewById(R.id.mapView)
        googleMapsManager.googleMapView = googleMapView
        mapButton = arLayout.findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            if (googleMapView.visibility == View.VISIBLE) {
                googleMapView.visibility = View.GONE
            } else {
                googleMapView.visibility = View.VISIBLE
            }
        }

        anchorToggleButton = arLayout.findViewById(R.id.anchorToggle)
        anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
            anchorIsChecked = isChecked
            googleMapsManager.updateAnchor(anchorIsChecked)
            if (!isChecked) {
                anchored = false
                Log.d(TAG, "AnchorToggle Disabled")
                //anchorDeltaTv.visibility = View.GONE
                fmLocationManager.unsetAnchor()
                googleMapsManager.unsetAnchor()
            }
        }

        qrToggleButton = arLayout.findViewById(R.id.qrToggle)
        qrToggleButton.setOnClickListener {
            val qrOverlay = arLayout.findViewById(R.id.qrOverlay) as ConstraintLayout
            if(qrOverlay.visibility == View.GONE){
                qrCodeReaderEnabled = true
                Log.d(TAG, "QR Code Reader Enabled")
                qrOverlay.visibility = View.VISIBLE
            }else{
                qrCodeReaderEnabled = false
                Log.d(TAG, "QR Code Reader Disabled")
                qrOverlay.visibility = View.GONE
            }
        }
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
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                Log.d(TAG, error.message.toString())
                (context as Activity).runOnUiThread {
                    lastResultTv.text = error.message.toString()
                }
            }

            override fun locationManager(result: FMLocationResult) {
                Log.d(TAG, result.confidence.toString())
                Log.d(TAG, result.location.toString())
                val stringResult =
                    "${result.location.coordinate.latitude},\n${result.location.coordinate.longitude} (${result.confidence})"
                (context as Activity).runOnUiThread {
                    lastResultTv.text = stringResult

                    googleMapsManager.addCorrespondingMarkersToMap(
                        result.location.coordinate.latitude,
                        result.location.coordinate.longitude
                    )
                }
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
                behaviorReceived = System.nanoTime()
                Log.d(TAG, "FrameFilterResult " + didRequestBehavior.displayName)
                val stringResult = didRequestBehavior.displayName
                (context as Activity).runOnUiThread {
                    filterRejectionTv.text = stringResult
                    if(filterRejectionTv.visibility == View.GONE){
                        filterRejectionTv.visibility = View.VISIBLE
                    }
                }
            }

            override fun locationManager(didChangeState: FMLocationManager.State) {
                (context as Activity).runOnUiThread {
                    when (didChangeState) {
                        FMLocationManager.State.LOCALIZING -> {
                            statusTv.setTextColor(Color.GREEN)
                        }
                        FMLocationManager.State.UPLOADING -> {
                            statusTv.setTextColor(Color.RED)
                        }
                        else -> {
                            statusTv.setTextColor(Color.BLACK)
                        }
                    }
                    statusTv.text = didChangeState.toString()
                }
            }

            override fun locationManager(
                didUpdateFrame: Frame,
                info: AccumulatedARCoreInfo,
                rejections: FrameFilterRejectionStatistics
            ) {
                (context as Activity).runOnUiThread {
                    normalTv.text =
                        info.trackingStateFrameStatistics.framesWithNormalTrackingState.toString()
                    limitedTv.text =
                        info.trackingStateFrameStatistics.framesWithLimitedTrackingState.toString()
                    notAvailableTv.text =
                        info.trackingStateFrameStatistics.framesWithNotAvailableTracking.toString()
                    excessiveMotionTv.text = rejections.excessiveMotionFrameCount.toString()
                    insufficientFeaturesTv.text = rejections.insufficientFeatures.toString()
                    pitchLowTv.text = rejections.excessiveTiltFrameCount.toString()
                    pitchHighTv.text = rejections.insufficientTiltFrameCount.toString()
                    blurryTv.text = rejections.excessiveBlurFrameCount.toString()
                    //tooFastTv.text = rejections.excessiveMotionFrameCount.toString()
                    tooLittleTv.text = rejections.insufficientMotionFrameCount.toString()
                    //featuresTv.text = rejections.insufficientFeatures.toString()
                    val stringDistance = String.format("%.2f", info.translationAccumulator.totalTranslation) + " m"
                    distanceTravelledTv.text = stringDistance
                    val stringSpread =
                        "[${info.rotationAccumulator.yaw[0]},${info.rotationAccumulator.yaw[1]}],${info.rotationAccumulator.yaw[2]}\n" +
                                "[${info.rotationAccumulator.pitch[0]},${info.rotationAccumulator.pitch[1]}],${info.rotationAccumulator.pitch[2]}\n" +
                                "[${info.rotationAccumulator.roll[0]},${info.rotationAccumulator.roll[1]}],${info.rotationAccumulator.roll[2]}"
                    cameraAnglesSpreadTv.text = stringSpread
                }
            }
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
        val cameraTranslation = frame.androidSensorPose?.translation
        cameraTranslationTv.text =
            createStringDisplay(cameraTranslation)

        val cameraRotation = frame.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay(cameraRotation)

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

        // Only read frame if the qrCodeReader is enabled and only if qrCodeReader is in reading mode
        if (qrCodeReaderEnabled && qrCodeReader.state != QRCodeReader.State.QRSCANNING) {
            frame.let { qrCodeReader.processImage(it) }
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

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Sets the [fmLocationManager.currentLocation] coordinates used to localize.
     */
    private fun getLocation() {
        if ((context.let {
                PermissionChecker.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
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

    fun updateLocation(latitude: Double, longitude: Double) {
        // Prevents fmLocationManager lateinit property not initialized
        if (this::fmLocationManager.isInitialized) {
            //Set SDK Location
            fmLocationManager.setLocation(
                latitude,
                longitude
            )
        } else {
            Log.e(
                TAG,
                "FMLocationManager not initialized: Please make sure connect() was invoked before updateLocation"
            )
        }
    }
}