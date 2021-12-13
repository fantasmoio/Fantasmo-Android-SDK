package com.example.fantasmo_android

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.fantasmo_android.helpers.SystemLocationListener
import com.example.fantasmo_android.helpers.SystemLocationManager
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse

class NoARCoreFragment : Fragment() {

    private val TAG = "NoARCoreFragment"

    private lateinit var currentView: View

    private lateinit var checkParkingButton: Button

    private lateinit var fmLocationManager: FMLocationManager

    // Host App location Manager to exemplify how to set Location
    private lateinit var systemLocationManager: SystemLocationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.noarcore_fragment, container, false)

        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)

        fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!

        useOwnLocationProvider()

        return currentView
    }

    /**
     * onStart lifecycle establishes listeners for the buttons and SDK calls
     * */
    override fun onStart() {
        super.onStart()

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
/*
            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 23) {
                Toast.makeText(
                    activity?.applicationContext,
                    "Is Zone In Radius Response: $it",
                    Toast.LENGTH_LONG
                ).show()
            }*/
        }
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(result: FMLocationResult) {
            }
        }

    /**
     * Example on how to override the internal location Manager
     */
    private fun useOwnLocationProvider() {
        systemLocationManager = SystemLocationManager(context, systemLocationListener)
    }

    private val systemLocationListener: SystemLocationListener =
        object : SystemLocationListener {
            override fun onLocationUpdate(currentLocation: Location) {
                fmLocationManager.setLocation(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    currentLocation.accuracy,
                    currentLocation.verticalAccuracyMeters
                )
            }
        }
}