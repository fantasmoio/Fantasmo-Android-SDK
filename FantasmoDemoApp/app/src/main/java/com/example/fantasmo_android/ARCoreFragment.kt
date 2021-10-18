package com.example.fantasmo_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.views.FMParkingView
import com.google.android.gms.location.*
import com.google.ar.core.*
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment()
{
    private val TAG = "ARCoreFragment"

    private lateinit var currentView: View

    private lateinit var fmParkingView: FMParkingView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        fmParkingView = currentView.findViewById(R.id.fmView)
        //fmParkingView.showStatistics = true
        //fmParkingView.isSimulation = true
        //fmParkingView.usesInternalLocationManager = true
        val appSessionId = UUID.randomUUID().toString()
        fmParkingView.connect("API_KEY",appSessionId)
        //fmParkingView.updateLocation(1.0,2.0)
        // Initiates GoogleMap display on UI from savedInstanceState from onCreateView method
        fmParkingView.initGoogleMap(savedInstanceState)

        return currentView
    }

    override fun onStart() {
        super.onStart()
        fmParkingView.onStart()
    }

    /**
     * Release heap allocation of the AR session
     */
    override fun onDestroy() {
        super.onDestroy()
        fmParkingView.onDestroy()
    }

    /**
     * Updates the state of the AR session
     */
    override fun onResume() {
        super.onResume()
        fmParkingView.onResume()
    }

    /**
     * Delivers the Google Maps API Key to the FMParkingView
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fmParkingView.onSaveInstanceStateApp(outState)
    }

    /**
     * Stops the AR session
     */
    override fun onStop() {
        super.onStop()
        fmParkingView.onStop()
    }

    /**
     * Pauses the AR session
     */
    override fun onPause() {
        super.onPause()
        fmParkingView.onPause()
    }

    /**
     * Makes sure GoogleMaps View follows every Android lifecycle
     * https://developers.google.com/android/reference/com/google/android/gms/maps/MapView#developer-guide
     */
    override fun onLowMemory() {
        super.onLowMemory()
        fmParkingView.onLowMemory()
    }
}