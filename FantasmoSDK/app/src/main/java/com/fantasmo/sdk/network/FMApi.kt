package com.fantasmo.sdk.network

import android.content.Context
import android.location.Location
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.*
import com.google.ar.core.Frame
import com.google.gson.Gson
import java.util.*

/**
 * Class to hold the necessary logic to communicate with Fantasmo API.
 */
class FMApi(
    private val fmNetworkManager: FMNetworkManager,
    private val fmLocationManager: FMLocationManager,
    private val context: Context,
    private val token: String,
) {

    /**
     * Method to build the Localize request.
     */
    fun sendLocalizeRequest(
        arFrame: Frame,
        onCompletion: (com.fantasmo.sdk.models.Location, List<FMZone>) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        try {
            fmNetworkManager.uploadImage(
                FMUtility.getImageDataFromARFrame(context, arFrame),
                getLocalizeParams(arFrame),
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
     */
    fun sendZoneInRadiusRequest(
        radius: Int,
        onCompletion: (Boolean) -> Unit
    ) {
        fmNetworkManager.zoneInRadiusRequest(
            "https://api.fantasmo.io/v1/parking.in.radius",
            getZoneInRadiusParams(radius),
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
        frame: Frame
    ): HashMap<String, String> {
        val pose = FMUtility.getPoseBasedOnDeviceOrientation(context, frame)

        val coordinates = if (fmLocationManager.isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            Coordinate(
                fmLocationManager.currentLocation.latitude,
                fmLocationManager.currentLocation.longitude
            )
        }

        val focalLength = frame.camera.imageIntrinsics.focalLength
        val principalPoint = frame.camera.imageIntrinsics.principalPoint
        val intrinsics = FMIntrinsics(
            focalLength.component1(),
            focalLength.component2(),
            principalPoint.component2(),
            principalPoint.component1()
        )

        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = System.currentTimeMillis().toString()
        params["gravity"] = gson.toJson(pose.orientation)
        params["uuid"] = UUID.randomUUID().toString()
        params["coordinate"] = gson.toJson(coordinates)
        params["intrinsics"] = gson.toJson(intrinsics)

        // calculate and send reference frame if anchoring
        val anchorFrame = fmLocationManager.anchorFrame
        if (anchorFrame != null) {
            params["referenceFrame"] =
                gson.toJson(FMUtility.anchorDeltaPoseForFrame(frame, anchorFrame))
        }

        return params
    }

    /**
     * Generate the zoneInRadius HTTP request parameters.
     * @param radius: search radius in meters
     * @return an HashMap with all the localization parameters.
     *
     * Only works with PARKING zones currently
     */
    private fun getZoneInRadiusParams(
        radius: Int,
    ): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        val coordinates = if (fmLocationManager.isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            Coordinate(
                fmLocationManager.currentLocation.latitude,
                fmLocationManager.currentLocation.longitude
            )
        }

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(coordinates)

        return params
    }
}