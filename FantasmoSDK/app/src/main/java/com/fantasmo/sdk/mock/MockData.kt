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

            if (zone == FMZone.ZoneType.parking) {
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

            return hashMapOf(
                "intrinsics" to Gson().toJson(intrinsic),
                "gravity" to Gson().toJson(gravity),
                "capturedAt" to System.currentTimeMillis().toString(),
                "uuid" to "C6241E04-974A-4131-8B36-044A11E2C7F0",
                "coordinate" to Gson().toJson(coordinate),
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

            return hashMapOf(
                "intrinsics" to Gson().toJson(intrinsic),
                "gravity" to Gson().toJson(gravity),
                "capturedAt" to System.currentTimeMillis().toString(),
                "uuid" to "A87E55CB-0649-4F87-A42F-8A33970F421E",
                "coordinate" to Gson().toJson(coordinate),
            )
        }

        private fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(
                CompressFormat.JPEG,
                FMUtility.Constants.JpegCompressionRatio,
                byteArrayOutputStream
            )
            return byteArrayOutputStream.toByteArray()
        }
    }
}