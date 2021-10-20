package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.views.FMParkingView
import com.google.android.gms.location.*
import com.google.ar.core.*
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment() {
    private val TAG = "ARCoreFragment"

    private lateinit var currentView: View

    private lateinit var fmParkingView: FMParkingView

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var simulationModeToggle: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showDebugStatsToggle: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        simulationModeToggle = currentView.findViewById(R.id.simulationModeToggle)
        simulationModeToggle.setOnCheckedChangeListener { _, checked ->
            fmParkingView.isSimulation = checked
        }

        // Enable Debug Mode to display session statistics
        showDebugStatsToggle = currentView.findViewById(R.id.showDebugStatsToggle)
        showDebugStatsToggle.setOnCheckedChangeListener { _, checked ->
            fmParkingView.showStatistics = checked
        }

        // Enable internal Location Manager
        fmParkingView.usesInternalLocationManager = true

        exitButton = currentView.findViewById(R.id.exitButton)
        exitButton.setOnClickListener{
            if(fmParkingView.visibility == View.VISIBLE){
                fmParkingView.visibility = View.GONE
                fmParkingView.disconnect()
                exitButton.visibility = View.GONE
                controlsLayout.visibility = View.VISIBLE
                Log.d(TAG,"END SESSION")
            }else{
                Log.d(TAG,"Exit Button Pressed")
            }
        }

        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener{
            if(fmParkingView.visibility == View.GONE){
                fmParkingView.visibility = View.VISIBLE
                val appSessionId = UUID.randomUUID().toString()
                fmParkingView.connect("API_KEY", appSessionId)
                //fmParkingView.updateLocation(1.0,2.0)
                // Initiates GoogleMap display on UI from savedInstanceState from onCreateView method
                fmParkingView.initGoogleMap(savedInstanceState)
                controlsLayout.visibility = View.GONE
                exitButton.visibility = View.VISIBLE
            }
        }

        return currentView
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