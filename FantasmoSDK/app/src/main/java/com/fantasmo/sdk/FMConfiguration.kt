package com.fantasmo.sdk

import android.location.Location
import com.fantasmo.sdk.fantasmosdk.BuildConfig

/**
 * Class to load configuration values like coordinates or server URLs.
 */
internal class FMConfiguration {

    companion object {
        /**
         * API base url
         */
        fun getServerURL(): String {
            return BuildConfig.FM_API_BASE_URL + "/v1/image.localize"
        }

        /**
         * API is_localization_available url
         */
        fun getIsLocalizationAvailableURL(): String {
            return BuildConfig.FM_API_BASE_URL + "/v2/isLocalizationAvailable"
        }

        /**
         * API initialize url
         */
        fun getInitializeURL(): String {
            return BuildConfig.FM_API_BASE_URL + "/v2/initialize"
        }

        /**
         * Current location
         */
        fun getConfigLocation(): Location {
            return if (BuildConfig.FM_GPS_LAT_LONG.isNotBlank()) {
                val locationComponents = BuildConfig.FM_GPS_LAT_LONG.split(",").toTypedArray()

                val location = Location("")
                location.latitude = locationComponents[0].toDouble()
                location.longitude = locationComponents[1].toDouble()

                location
            } else {
                Location("")
            }
        }

        fun getSessionAnalyticsURL(): String {
            return BuildConfig.FM_API_BASE_URL + "/v2/analytics/session"
        }
    }
}