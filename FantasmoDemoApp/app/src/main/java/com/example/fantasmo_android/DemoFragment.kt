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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMParkingViewProtocol
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class DemoFragment : Fragment() {
    private val TAG = DemoFragment::class.java.simpleName

    private lateinit var currentView: View

    private lateinit var controlsLayout: ConstraintLayout
    private lateinit var resultTextView: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var isSimulationSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showsStatisticsSwitch: Switch

    private lateinit var endRideButton: Button

    // Control variables for the FMParkingView
    private lateinit var fmParkingView: FMParkingView
    private val usesInternalLocationManager = true
    private val accessToken = "API_KEY"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.demo_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)
        resultTextView = currentView.findViewById(R.id.localizationResultView)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        // Assign a controller
        fmParkingView.fmParkingViewController = fmParkingViewController
        // Assign an accessToken
        fmParkingView.accessToken = accessToken

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        isSimulationSwitch = currentView.findViewById(R.id.simulationModeToggle)
        isSimulationSwitch.setOnCheckedChangeListener { _, checked ->
            fmParkingView.isSimulation = checked
        }

        // Enable Debug Mode to display session statistics
        showsStatisticsSwitch = currentView.findViewById(R.id.showDebugStatsToggle)
        showsStatisticsSwitch.setOnCheckedChangeListener { _, checked ->
            fmParkingView.showStatistics = checked
        }

        // Enable FMParkingView internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        handleFMParkingViewDismiss()

        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            handleEndRideButton()
        }

        return currentView
    }

    private fun handleEndRideButton() {
        // Test location of a parking space in Berlin
        val latitude = 52.50578283943285
        val longitude = 13.378954977173915
        // Before trying to localize with Fantasmo you should check if the user is near a mapped parking space
        fmParkingView.isParkingAvailable(latitude, longitude) {
            if (it) {
                startParkingFlow()
            } else {
                resultTextView.visibility = View.VISIBLE
                val stringNotAvailable = "Parking not available near your location."
                resultTextView.text = stringNotAvailable
            }
        }
    }

    private fun startParkingFlow() {
        // Display `FMParkingView` and initialize `sessionId`. This is typically a UUID string
        // but it can also follow your own format. It is used for analytics and billing purposes and
        // should represent a single parking session.
        val sessionId = UUID.randomUUID().toString()
        // Present the FMParkingView
        fmParkingView.connect(sessionId)
        resultTextView.visibility = View.GONE
        controlsLayout.visibility = View.INVISIBLE
    }

    private fun handleFMParkingViewDismiss() {
        fmParkingView.viewTreeObserver.addOnGlobalLayoutListener {
            if(fmParkingView.visibility == View.GONE){
                //visibility has changed
                Log.d(TAG,"FMParkingView Changed Visibility")
                controlsLayout.visibility = View.VISIBLE
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

            override fun fmParkingViewDidStopQRScanning() {
                Log.d(TAG, "QR Code Reader Disabled")
            }

            override fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit) {
                Log.d(TAG, "QR Code Scan Successful")
                // Optional validation of the QR code can be done here
                // Note: If you choose to implement this method, you must call the `onValidQRCode` with the validation result
                // show dialogue to accept or refuse
                onValidQRCode(true)
            }

            override fun fmParkingViewDidStartLocalizing() {
                Log.d(TAG, "Started Localizing")
            }

            override fun fmParkingView(behavior: FMBehaviorRequest) {
                Log.d(TAG, "Received Behavior: ${behavior.description}")
            }

            override fun fmParkingView(result: FMLocationResult) {
                // Got a localization result
                // Localization will continue until you dismiss the view
                // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
                when (result.confidence) {
                    FMResultConfidence.LOW -> {
                        Log.d(TAG, "LOW Confidence Result")
                    }
                    FMResultConfidence.MEDIUM -> {
                        Log.d(TAG, "MEDIUM Confidence Result")
                    }
                    FMResultConfidence.HIGH -> {
                        Log.d(TAG, "HIGH Confidence Result")
                        fmParkingView.dismiss()
                        val stringResult = "Result: ${result.location.coordinate} (${result.confidence})"
                        resultTextView.text = stringResult
                        if(resultTextView.visibility == View.GONE){
                            resultTextView.visibility = View.VISIBLE
                        }

                    }
                }
            }

            override fun fmParkingView(error: ErrorResponse, metadata: Any?) {
                Log.e(TAG, "Received Error: $error")
            }
        }
}