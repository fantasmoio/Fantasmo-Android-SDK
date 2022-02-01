//
//  MockData.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.mock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.BitmapFactory
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.network.FMLocalizationRequest
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

/**
 * Class to hold utilitary methods to create mock data for the request.
 */
class MockData {
    companion object {

        /**
         * Generate a simulated localization query using stored images from
         * a known location.
         * @param zone: Type of semantic zone to simulate.
         * @param isValid: True if attempt should succeed. False if attempt should fail.
         * @param context: the application context to get the image from resources.
         * @return a Pair with the parameters HasMap and a ByteArray of the image data for query.
         */
        fun simulateLocalizeRequest(
            zone: FMZone.ZoneType,
            isValid: Boolean,
            context: Context
        ): Pair<Map<String, Any>?, ByteArray?> {
            val params: Map<String, Any>?
            val jpegData: ByteArray?

            if (zone == FMZone.ZoneType.PARKING) {
                params = parkingMockParameters()

                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_in_parking)
                jpegData = getFileDataFromDrawable(bitmap)
            } else {
                params = streetMockParameters()

                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_on_street)
                jpegData = getFileDataFromDrawable(bitmap)
            }

            return Pair(params, jpegData)
        }

        /**
         * Generate a simulated localization query params from a known location.
         * @param request Request that specifies the type of semantic zone to simulate.
         * @return Parameters for query.
         */
        fun params(request: FMLocalizationRequest): HashMap<String, String> {
            return when(request.simulationZone){
                FMZone.ZoneType.PARKING -> {
                    parkingMockParameters()
                }
                else -> {
                    streetMockParameters()
                }
            }
        }

        private fun parkingMockParameters(): HashMap<String, String> {
            val intrinsic = hashMapOf(
                "fx" to 1211.782470703125,
                "fy" to 1211.9073486328125,
                "cx" to 1017.4938354492188,
                "cy" to 788.2992553710938
            )

            val gravity = hashMapOf(
                "w" to 0.7729115057076497,
                "x" to 0.026177782246603,
                "y" to 0.6329531644390612,
                "z" to -0.03595580186787759
            )

            val coordinate = Coordinate(48.12844364094412, 11.572596873561112)
            val location = Location(0, 0, 0, 0, coordinate)

            return hashMapOf(
                "intrinsics" to Gson().toJson(intrinsic),
                "gravity" to Gson().toJson(gravity),
                "location" to Gson().toJson(location),
            )
        }

        private fun streetMockParameters(): HashMap<String, String> {
            val intrinsic = hashMapOf(
                "fx" to 1036.486083984375,
                "fy" to 1036.486083984375,
                "cx" to 480.23284912109375,
                "cy" to 628.2947998046875
            )

            val gravity = hashMapOf(
                "w" to 0.7634205318288221,
                "x" to 0.05583266127506817,
                "y" to 0.6407979294057553,
                "z" to -0.058735161414937516
            )

            val coordinate = Coordinate(48.12844364094412, 11.572596873561112)
            val location = Location(0, 0, 0, 0, coordinate)

            return hashMapOf(
                "intrinsics" to Gson().toJson(intrinsic),
                "gravity" to Gson().toJson(gravity),
                "location" to Gson().toJson(location),
            )
        }

        fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(
                CompressFormat.JPEG,
                FMUtility.Constants.JpegCompressionRatio,
                byteArrayOutputStream
            )
            return byteArrayOutputStream.toByteArray()
        }

        /**
         * Return a simulated localization images from a known location.
         * @param request: FMLocalizeRequest that contains Type of semantic zone to simulate
         * @param context: App Context
         * @return result: ByteArray containing encoded image data for query.
         */
        fun imageData(request: FMLocalizationRequest, context: Context): ByteArray {
            return if (request.simulationZone == FMZone.ZoneType.PARKING) {
                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_in_parking)
                getFileDataFromDrawable(bitmap)
            } else {
                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_on_street)
                return getFileDataFromDrawable(bitmap)
            }
        }

        /**
         * Return a simulated localization images from a known location.
         * @param request: FMLocalizeRequest that contains Type of semantic zone to simulate
         * @param context: App Context
         * @return result: IntArray containing image resolution data for query.
         */
        fun getImageResolution(request: FMLocalizationRequest, context: Context): IntArray {
            return if (request.simulationZone == FMZone.ZoneType.PARKING) {
                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_in_parking)
                intArrayOf(bitmap.height, bitmap.width)
            } else {
                val bitmap =
                    BitmapFactory.decodeResource(context.resources, R.drawable.image_on_street)
                intArrayOf(bitmap.height, bitmap.width)
            }
        }
    }
}