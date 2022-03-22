package com.fantasmo.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import android.util.Log
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.evaluators.FMFrameEvaluationType
import com.fantasmo.sdk.fantasmosdk.BuildConfig
import com.fantasmo.sdk.mock.MockData
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.models.analytics.MagneticField
import com.google.gson.Gson
import java.util.*

/**
 * Class to hold a LocalizationRequest
 */
class FMLocalizationRequest(
    var isSimulation: Boolean,
    var simulationZone: FMZone.ZoneType,
    var location: Location,
    var relativeOpenCVAnchorPose: FMPose?,
    var analytics: FMLocalizationAnalytics
)

/**
 * Class to hold all the Localization Analytics
 */
class FMLocalizationAnalytics(
    var appSessionId: String?,
    var appSessionTags: List<String>?,
    var localizationSessionId: String?,
    var frameEvents: FMFrameEvent,
    var rotationSpread: FMRotationSpread,
    var totalDistance: Float,
    var magneticField: MagneticField,
    var imageEnhancementInfo: FMImageEnhancementInfo?,
    var imageQualityFilterInfo: FMImageQualityFilterInfo?,
    var remoteConfigId: String
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
 * Class to hold image enhancement info
 */
class FMImageEnhancementInfo(
    var gamma: Float
)

/**
 * Class to hold the necessary logic to communicate with Fantasmo API.
 */
class FMApi(
    private val context: Context,
    private val token: String,
) {
    var fmNetworkManager = FMNetworkManager(context)

    private val TAG = FMApi::class.java.simpleName

    /**
     * Method to build the Localize request.
     */
    fun sendLocalizeRequest(
        fmFrame: FMFrame,
        request: FMLocalizationRequest,
        onCompletion: (Location, List<FMZone>) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        try {
            val imageData = imageData(fmFrame, request) ?: error("No image data to send in request")
            fmNetworkManager.uploadImage(
                FMConfiguration.getServerURL(),
                imageData,
                getLocalizeParams(fmFrame, request),
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
     * Method to build the IsLocalizationAvailable request.
     * @param location Location to search
     */
    fun sendIsLocalizationAvailable(
        location: Location,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        fmNetworkManager.isLocalizationAvailableRequest(
            FMConfiguration.getIsLocalizationAvailableURL(),
            getIsLocalizationAvailableParams(location),
            token,
            onCompletion,
            onError
        )
    }

    /**
     * Generate the isLocalizationAvailable HTTP request parameters.
     * @param location Location to search
     * @return an HashMap with all the location parameters.
     */
    private fun getIsLocalizationAvailableParams(
        location: Location,
    ): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        params["location"] = Gson().toJson(location)

        Log.i(TAG, "getIsLocalizationAvailableParams: $params")
        return params
    }

    /**
     * Method to build the Initialize request.
     * @param location Location to search
     */
    fun sendInitializationRequest(
        location: Location,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        fmNetworkManager.sendInitializationRequest(
            FMConfiguration.getInitializeURL(),
            getInitializationParams(location),
            token,
            onCompletion,
            onError
        )
    }

    /**
     * Generate the initialize HTTP request parameters.
     * @param location Location to search
     * @return an HashMap with all the location parameters.
     */
    private fun getInitializationParams(
        location: Location
    ): HashMap<String, String> {
        val params = hashMapOf<String, String>()
        val gson = Gson()
        params += getDeviceAndHostAppInfo()
        params["location"] = gson.toJson(location)

        Log.i(TAG, "getInitializationRequest: $params")
        return params
    }

    /**
     * Generate the localize HTTP request parameters. Can fail if the jpeg
     * conversion throws an exception.
     * @param frame Frame to localize
     * @return an HashMap with all the localization parameters.
     */
    private fun getLocalizeParams(
        fmFrame: FMFrame,
        request: FMLocalizationRequest
    ): HashMap<String, String> {
        val pose = FMUtility.getPoseOfOpenCVVirtualCameraBasedOnDeviceOrientation(fmFrame)

        val location = request.location

        val resolution = hashMapOf<String, Int>()
        val imageResolution = getImageResolution(fmFrame, request)
        resolution["height"] = imageResolution.height
        resolution["width"] = imageResolution.width

        val focalLength = fmFrame.camera.imageIntrinsics.focalLength
        val principalPoint = fmFrame.camera.imageIntrinsics.principalPoint
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

        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = System.currentTimeMillis().toString()
        params["gravity"] = gson.toJson(pose.orientation)
        params["uuid"] = UUID.randomUUID().toString()
        params["location"] = gson.toJson(location)
        params["intrinsics"] = gson.toJson(intrinsics)
        params["imageResolution"] = gson.toJson(resolution)

        // session identifiers
        params["appSessionId"] = request.analytics.appSessionId!!
        val appSessionTags = request.analytics.appSessionTags
        params["appSessionTags"] = if (appSessionTags != null) {
             gson.toJson(appSessionTags)
        } else {
            ""
        }

        params["localizationSessionId"] = request.analytics.localizationSessionId!!

        // other analytics
        params["frameEventCounts"] = gson.toJson(frameEventCounts)
        params["totalDistance"] = request.analytics.totalDistance.toString()
        params["rotationSpread"] = gson.toJson(request.analytics.rotationSpread)
        params["magneticData"] = gson.toJson(request.analytics.magneticField)


        // add frame evaluation info, if available
        if (fmFrame.evaluation != null) {
            params["frameEvaluation"] = gson.toJson(fmFrame.evaluation)
        }

        if(request.analytics.imageEnhancementInfo != null) {
            params["imageEnhancementInfo"] = gson.toJson(request.analytics.imageEnhancementInfo)
        }

        params["remoteConfigId"] = gson.toJson(request.analytics.remoteConfigId)

        // calculate and send reference frame if anchoring
        val relativeOpenCVAnchorPose = request.relativeOpenCVAnchorPose
        if (relativeOpenCVAnchorPose != null) {
            params["referenceFrame"] = gson.toJson(relativeOpenCVAnchorPose)
        }

        // add device and host app info
        params += getDeviceAndHostAppInfo()

        // add fixed simulated data if simulating
        if (request.isSimulation) {
            params += MockData.params(request)
        }

        Log.i(TAG, "getLocalizeParams")
        return params
    }

    /**
     * Generate the image data used to perform "localize" HTTP request.
     * @param arFrame Frame to localize
     * @param request FMLocalizationRequest with information about simulation mode
     * @return result ByteArray with image to localize
     */
    private fun imageData(fmFrame: FMFrame, request: FMLocalizationRequest): ByteArray? {
        if (request.isSimulation) {
            return MockData.imageData(request, context)
        }
        return fmFrame.imageData()
    }

    /**
     * Get the image resolution used to perform "localize" HTTP request.
     * @param fmFrame Frame to return the resolution from
     * @param request Localization request struct
     * @return result Resolution of the frame
     */
    private fun getImageResolution(
        fmFrame: FMFrame,
        request: FMLocalizationRequest
    ): FMFrameResolution {
        if (request.isSimulation) {
            val result = MockData.getImageResolution(request, context)
            return FMFrameResolution(result[0], result[1])
        }
        val height = fmFrame.camera.imageIntrinsics.imageDimensions[0]
        val width = fmFrame.camera.imageIntrinsics.imageDimensions[1]
        return FMFrameResolution(height, width)
    }

    /**
     * Returns a dictionary of common device and host app info that can be added to request parameters
     */
    @SuppressLint("HardwareIds")
    private fun getDeviceAndHostAppInfo(): HashMap<String, String> {
        val androidId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        val manufacturer = Build.MANUFACTURER // Samsung
        val model = Build.MODEL  // SM-G780
        val deviceModel = "$manufacturer $model" // Samsung SM-G780
        val deviceOsVersion = Build.VERSION.SDK_INT.toString() // "30" (Android 11)
        val fantasmoSdkVersion = BuildConfig.VERSION_NAME // "1.0.5"

        val params = hashMapOf<String, String>()
        // device characteristics
        params["udid"] = androidId
        params["deviceModel"] = deviceModel
        params["deviceOs"] = "android"
        params["deviceOsVersion"] = deviceOsVersion
        params["sdkVersion"] = fantasmoSdkVersion

        return params
    }
}
