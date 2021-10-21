package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMQRScanningViewProtocol
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

    private lateinit var fmParkingView: FMParkingView

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var simulationModeToggle: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showDebugStatsToggle: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button

    private lateinit var qrCodeResultTv: TextView
    private lateinit var qrOverlay: ConstraintLayout

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
        exitButton.setOnClickListener {
            if (fmParkingView.visibility == View.VISIBLE) {
                fmParkingView.visibility = View.GONE
                fmParkingView.disconnect()
                exitButton.visibility = View.GONE
                controlsLayout.visibility = View.VISIBLE
                Log.d(TAG, "END SESSION")
            }
        }

        val latitude = 52.50578283943285
        val longitude = 13.378954977173915
        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            fmParkingView.isParkingAvailable(latitude, longitude, accessToken) {
                if (it) {
                    if (fmParkingView.visibility == View.GONE) {
                        fmParkingView.visibility = View.VISIBLE
                        val appSessionId = UUID.randomUUID().toString()
                        fmParkingView.connect(accessToken, appSessionId)
                        //fmParkingView.updateLocation(1.0,2.0)
                        // Initiates GoogleMap display on UI from savedInstanceState from onCreateView method
                        fmParkingView.initGoogleMap(savedInstanceState)
                        controlsLayout.visibility = View.GONE
                        exitButton.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        context?.applicationContext,
                        "Is Zone In Radius Response: $it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        qrCodeResultTv = currentView.findViewById(R.id.qrCodeResultTextView)
        qrOverlay = currentView.findViewById(R.id.qrOverlay) as ConstraintLayout

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

    /**
     * Listener for the QR Code Scanner.
     */
    private val fmParkingViewController: FMParkingViewProtocol =
        object : FMParkingViewProtocol {
            override fun fmParkingViewDidStartQRScanning() {
                Log.d(TAG, "QR Code Reader Enabled")
                if (qrCodeResultTv.visibility == View.GONE) {
                    qrCodeResultTv.visibility = View.VISIBLE
                }
                if (qrOverlay.visibility == View.GONE) {
                    qrOverlay.visibility = View.VISIBLE
                }
            }

            override fun fmParkingViewDidStopQRScanning(){}
            override fun fmParkingView(qrCode: String, shouldContinue: (Boolean) -> Unit){
                // Display result and approve or not the QR Code Scanned
                qrCodeResultTv.text = qrCode
                if (qrOverlay.visibility == View.VISIBLE) {
                    qrOverlay.visibility = View.GONE
                }
                shouldContinue(true)
                // show dialogue to accept or refuse
            }

            override fun fmParkingViewDidStartLocalizing(){
                Log.d(TAG,"BEGINNING LOCALIZING")
            }
            override fun fmParkingView(behavior: FMBehaviorRequest){}
            override fun fmParkingView(result: FMLocationResult){}
            override fun fmParkingView(error: ErrorResponse, metadata: Any?){}
        }
}