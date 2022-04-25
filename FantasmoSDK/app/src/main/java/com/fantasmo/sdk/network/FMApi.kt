package com.fantasmo.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.FMDeviceAndHostInfo
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.mock.MockData
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.models.analytics.FMFrameResolution
import com.fantasmo.sdk.models.analytics.FMLocalizationAnalytics
import com.fantasmo.sdk.models.analytics.FMSessionAnalytics
import com.google.gson.Gson
import java.util.*
import kotlin.collections.HashMap


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
     * Method to send the analytics of a localization session.
     * @param sessionAnalytics data model containing the session analytics
     */
    fun stopOngoingLocalizeRequests(
    ) {
        fmNetworkManager.stopAllLocalizeRequests()
    }

    /**
     * Method to send the analytics of a localization session.
     * @param sessionAnalytics data model containing the session analytics
     */
    fun sendSessionAnalyticsRequest(
        sessionAnalytics: FMSessionAnalytics,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        fmNetworkManager.sendSessionAnalyticsRequest(
            FMConfiguration.getSessionAnalyticsURL(),
            getSessionAnalyticsParams(sessionAnalytics),
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
     * Generate the analytics HTTP request parameters.
     * @param sessionAnalytics Location to search
     * @return an HashMap with all the location parameters.
     */
    private fun getSessionAnalyticsParams(
        sessionAnalytics: FMSessionAnalytics
    ): String{
        val gson = Gson()
        val json = gson.toJson(sessionAnalytics)
        Log.i(TAG, "sessionAnalyticsRequest: $json")
        return json
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

        val events = request.analytics.legacyFrameEvents
        val frameEventCounts = hashMapOf<String, String>()
        frameEventCounts["excessiveTilt"] = events.excessiveTilt.toString()
        frameEventCounts["excessiveBlur"] = events.excessiveBlur.toString()
        frameEventCounts["excessiveMotion"] = events.excessiveMotion.toString()
        frameEventCounts["insufficientFeatures"] = events.insufficientFeatures.toString()
        frameEventCounts["lossOfTracking"] = events.lossOfTracking.toString()
        frameEventCounts["total"] = events.total.toString()

        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = (System.currentTimeMillis().toDouble() / 1000.0).toString()
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
        val deviceAndHostInfo = FMDeviceAndHostInfo(context)

        val params = hashMapOf<String, String>()
        // device characteristics
        params["udid"] = deviceAndHostInfo.udid
        params["deviceModel"] = deviceAndHostInfo.deviceModel
        params["deviceOs"] = deviceAndHostInfo.deviceOs
        params["deviceOsVersion"] = deviceAndHostInfo.deviceOsVersion
        params["sdkVersion"] = deviceAndHostInfo.sdkVersion
        params["hostAppMarketingVersion"] = deviceAndHostInfo.hostAppMarketingVersion
        params["hostAppBuild"] = deviceAndHostInfo.hostAppBuild
        params["hostAppBundleIdentifier"] = deviceAndHostInfo.hostAppBundleIdentifier
        return params
    }
}
