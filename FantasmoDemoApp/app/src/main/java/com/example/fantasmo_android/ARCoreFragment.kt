package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMParkingViewProtocol

import com.google.android.gms.location.*
import com.google.ar.core.*

import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment() {
    private val TAG = "ARCoreFragment"

    private lateinit var currentView: View

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var simulationModeToggle: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showDebugStatsToggle: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button

    // Host App location Manager to exemplify how to set Location
    private lateinit var systemLocationManager: SystemLocationManager

    // Control variables for the FMParkingView
    private lateinit var fmParkingView: FMParkingView
    private val usesInternalLocationManager = true
    private val accessToken = "API_KEY"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        fmParkingView.fmParkingViewController = fmParkingViewController
        fmParkingView.appSessionId = UUID.randomUUID().toString()
        fmParkingView.accessToken = accessToken

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

        // Enable FMParkingView internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        handleExitButton()

        val latitude = 52.50578283943285
        val longitude = 13.378954977173915
        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            fmParkingView.isParkingAvailable(latitude, longitude) {
                if (it) {
                    startParkingFlow()
                } else {
                    Toast.makeText(
                        context?.applicationContext,
                        "Parking not available near your location.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        return currentView
    }

    private fun startParkingFlow() {
        if (fmParkingView.visibility == View.GONE) {
            fmParkingView.visibility = View.VISIBLE
            // Present the FMParkingView
            fmParkingView.present()
            useOwnLocationProvider()
            controlsLayout.visibility = View.GONE
            exitButton.visibility = View.VISIBLE
        }
    }

    /**
     * Example on how to override the internal location Manager
     */
    private fun useOwnLocationProvider() {
        if(!usesInternalLocationManager){
            systemLocationManager = SystemLocationManager(context,systemLocationListener)
        }
    }

    private val systemLocationListener: SystemLocationListener =
        object : SystemLocationListener{
            override fun onLocationUpdate(currentLocation: Location) {
                fmParkingView.updateLocation(currentLocation.latitude,currentLocation.longitude)
            }
        }

    private fun handleExitButton() {
        exitButton = currentView.findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            if (fmParkingView.visibility == View.VISIBLE) {
                fmParkingView.visibility = View.GONE
                // When exit Button is clicked close the session
                fmParkingView.disconnect()
                exitButton.visibility = View.GONE
                controlsLayout.visibility = View.VISIBLE
                Log.d(TAG, "END SESSION")
            }
        }
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
     * Pauses the AR session
     */
    override fun onPause() {
        super.onPause()
        fmParkingView.onPause()
    }

    /**
     * Listener for the FMParkingView.
     */
    private val fmParkingViewController: FMParkingViewProtocol =
        object : FMParkingViewProtocol {
            override fun fmParkingViewDidStartQRScanning() {
                Log.d(TAG, "QR Code Reader Enabled")
            }

            override fun fmParkingViewDidStopQRScanning(){}
            override fun fmParkingView(qrCode: String, shouldContinue: (Boolean) -> Unit){
                // Optional validation of the QR code can be done here
                // Note: If you choose to implement this method, you must call the `shouldContinue` with the validation result
                // show dialogue to accept or refuse
                shouldContinue(true)
            }

            override fun fmParkingViewDidStartLocalizing(){
                Log.d(TAG,"BEGINNING LOCALIZING")
            }
            override fun fmParkingView(behavior: FMBehaviorRequest){
            }
            override fun fmParkingView(result: FMLocationResult){
                // Got a localization result
                // Localization will continue until you dismiss the view
                // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
                if(result.confidence == FMResultConfidence.LOW){
                    Log.d(TAG,"Low Confidence Result")
                }
            }
            override fun fmParkingView(error: ErrorResponse, metadata: Any?){}
        }
}