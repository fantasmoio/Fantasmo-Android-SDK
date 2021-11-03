package com.fantasmo.sdk.network

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import android.util.Log
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.fantasmosdk.BuildConfig
import com.fantasmo.sdk.mock.MockData
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.models.analytics.MagneticField
import com.google.ar.core.Frame
import com.google.gson.Gson
import java.util.*

/**
 * Class to hold a LocalizationRequest
 */
class FMLocalizationRequest(
    var isSimulation: Boolean,
    var simulationZone: FMZone.ZoneType,
    var coordinate: Coordinate,
    var relativeOpenCVAnchorPose: FMPose?,
    var analytics: FMLocalizationAnalytics
)

/**
 * Class to hold all the Localization Analytics
 */
class FMLocalizationAnalytics(
    var appSessionId: String,
    var localizationSessionId: String,
    var frameEvents: FMFrameEvent,
    var rotationSpread: FMRotationSpread,
    var totalDistance: Float,
    var magneticField: MagneticField
)

/**
 * Class to hold all the frame events during a localization session
 */
class FMFrameEvent(
    var excessiveTilt: Int,
    var excessiveBlur: Int,
    var excessiveMotion: Int,
    var insufficientFeatures: Int,
    var lossOfTracking: Int,
    var total: Int
)

/**
 * Class to hold rotation spread during a localization session
 */
class FMRotationSpread(
    var pitch: Float,
    var yaw: Float,
    var roll: Float
)

/**
 * Class to hold image resolution
 */
class FMFrameResolution(
    var height: Int,
    var width: Int
)

/**
 * Class to hold the necessary logic to communicate with Fantasmo API.
 */
class FMApi(
    private val context: Context,
    private val token: String,
) {
    var fmNetworkManager = FMNetworkManager(FMConfiguration.getServerURL(), context)

    private val TAG = "FMApi"

    /**
     * Method to build the Localize request.
     */
    fun sendLocalizeRequest(
        arFrame: Frame,
        request: FMLocalizationRequest,
        onCompletion: (Location, List<FMZone>) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        try {
            fmNetworkManager.uploadImage(
                imageData(arFrame, request),
                getLocalizeParams(arFrame, request),
                token,
                {
                    val location = it.location
                    val geofences = it.geofences

                    val fmZones = mutableListOf<FMZone>()
                    if (geofences != null && geofences.isNotEmpty()) {
                        for (geofence in geofences) {
                            val fmZone = FMZone(
                                FMZone.ZoneType.valueOf(geofence.elementType),
                                geofence.elementID.toString()
                            )
                            fmZones.add(fmZone)
                        }
                    }
                    location?.let { localizeResponse ->
                        onCompletion(localizeResponse, fmZones)
                    }
                },
                {
                    onError(it)
                })
        } catch (e: Exception) {
            onError(ErrorResponse(0, e.message))
        }
    }

    /**
     * Method to build the ZoneInRadius request.
     * @param latitude: Location latitude to search
     * @param longitude: Location longitude to search
     * @param radius: search radius in meters
     */
    fun sendZoneInRadiusRequest(
        latitude: Double,
        longitude: Double,
        radius: Int,
        onCompletion: (Boolean) -> Unit
    ) {
        fmNetworkManager.zoneInRadiusRequest(
            "https://api.fantasmo.io/v1/parking.in.radius",
            getZoneInRadiusParams(latitude, longitude, radius),
            token,
            onCompletion
        )
    }

    /**
     * Generate the localize HTTP request parameters. Can fail if the jpeg
     * conversion throws an exception.
     * @param frame: Frame to localize
     * @return an HashMap with all the localization parameters.
     */
    private fun getLocalizeParams(
        frame: Frame,
        request: FMLocalizationRequest
    ): HashMap<String, String> {
        val pose = FMUtility.getPoseOfOpenCVVirtualCameraBasedOnDeviceOrientation(context, frame)

        val coordinates = if (request.isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            request.coordinate
        }

        val resolution = hashMapOf<String, Int>()
        val imageResolution = getImageResolution(frame,request)
        resolution["height"] = imageResolution.height
        resolution["width"] = imageResolution.width

        val focalLength = frame.camera.imageIntrinsics.focalLength
        val principalPoint = frame.camera.imageIntrinsics.principalPoint
        val intrinsics = FMIntrinsics(
            focalLength.component1(),
            focalLength.component2(),
            principalPoint.component2(),
            principalPoint.component1()
        )

        val events = request.analytics.frameEvents
        val frameEventCounts = hashMapOf<String, String>()
        frameEventCounts["excessiveTilt"] = events.excessiveTilt.toString()
        frameEventCounts["excessiveBlur"] = events.excessiveBlur.toString()
        frameEventCounts["excessiveMotion"] = events.excessiveMotion.toString()
        frameEventCounts["insufficientFeatures"] = events.insufficientFeatures.toString()
        frameEventCounts["lossOfTracking"] = events.lossOfTracking.toString()
        frameEventCounts["total"] = events.total.toString()

        val androidId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        val manufacturer = Build.MANUFACTURER // Samsung
        val model = Build.MODEL  // SM-G780
        val deviceModel = "$manufacturer $model" // Samsung SM-G780
        val deviceOsVersion = Build.VERSION.SDK_INT.toString() // "30" (Android 11)
        val fantasmoSdkVersion = BuildConfig.VERSION_NAME // "1.0.5"

        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = System.currentTimeMillis().toString()
        params["gravity"] = gson.toJson(pose.orientation)
        params["uuid"] = UUID.randomUUID().toString()
        params["coordinate"] = gson.toJson(coordinates)
        params["intrinsics"] = gson.toJson(intrinsics)
        params["imageResolution"] = gson.toJson(resolution)

        // device characteristics
        params["udid"] = androidId
        params["deviceModel"] = deviceModel
        params["deviceOs"] = "android"
        params["deviceOsVersion"] = deviceOsVersion
        params["sdkVersion"] = fantasmoSdkVersion

        // session identifiers
        params["appSessionId"] = request.analytics.appSessionId
        params["localizationSessionId"] = request.analytics.localizationSessionId

        // other analytics
        params["frameEventCounts"] = gson.toJson(frameEventCounts)
        params["totalDistance"] = request.analytics.totalDistance.toString()
        params["rotationSpread"] = gson.toJson(request.analytics.rotationSpread)
        params["magneticData"] = gson.toJson(request.analytics.magneticField)

        // calculate and send reference frame if anchoring
        val relativeOpenCVAnchorPose = request.relativeOpenCVAnchorPose
        if (relativeOpenCVAnchorPose != null) {
            params["referenceFrame"] = gson.toJson(relativeOpenCVAnchorPose)
        }

        Log.i(TAG, "getLocalizeParams")
        return params
    }

    /**
     * Generate the zoneInRadius HTTP request parameters.
     * @param latitude: Location latitude to search
     * @param longitude: Location longitude to search
     * @param radius: search radius in meters
     * @return an HashMap with all the localization parameters.
     *
     * Only works with PARKING zones currently
     */
    private fun getZoneInRadiusParams(
        latitude: Double,
        longitude: Double,
        radius: Int,
    ): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        val coordinates = Coordinate(latitude, longitude)

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(coordinates)

        Log.i(TAG, "getZoneInRadiusParams: $params")
        return params
    }

    /**
     * Generate the image data used to perform "localize" HTTP request.
     * @param arFrame: Frame to localize
     * @param request: FMLocalizationRequest with information about simulation mode
     * @return result: ByteArray with image to localize
     */
    private fun imageData(arFrame: Frame, request: FMLocalizationRequest): ByteArray {
        return FMUtility.getImageDataFromARFrame(context, arFrame)
    }

    /**
     * Get the image resolution used to perform "localize" HTTP request.
     * @param arFrame: Frame to return the resolution from
     * @param request: Localization request struct
     * @return result: Resolution of the frame
     */
    private fun getImageResolution(arFrame: Frame, request: FMLocalizationRequest): FMFrameResolution{
        val height = arFrame.camera.imageIntrinsics.imageDimensions[0]
        val width = arFrame.camera.imageIntrinsics.imageDimensions[1]
        return FMFrameResolution(height,width)
    }
}