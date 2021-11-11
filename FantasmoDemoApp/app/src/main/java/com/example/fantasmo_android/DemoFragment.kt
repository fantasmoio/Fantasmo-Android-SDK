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
    private lateinit var resultsLayout: ConstraintLayout
    private lateinit var resultTextView: TextView
    private lateinit var mapPinButton: Button
    private lateinit var mapFragment: MapFragment
    private lateinit var lastResult: FMLocationResult

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var isSimulationSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showStatisticsSwitch: Switch

    private lateinit var endRideButton: Button

    // Control variables for the FMParkingView
    private lateinit var fmParkingView: FMParkingView

    // Tell the FMParkingView to use or not its' internal location manager
    private val usesInternalLocationManager = true

    // FMParkingView accessToken
    private val accessToken = "API_KEY"

    private var hasRequestedEndRide = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.demo_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)
        resultsLayout = currentView.findViewById(R.id.resultsLayout)
        mapPinButton = currentView.findViewById(R.id.mapPinButton)
        mapPinButton.setOnClickListener {
            openMap()
        }

        resultTextView = currentView.findViewById(R.id.localizationResultView)
        isSimulationSwitch = currentView.findViewById(R.id.simulationModeSwitch)
        showStatisticsSwitch = currentView.findViewById(R.id.showStatisticsSwitch)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        // Assign an accessToken
        fmParkingView.accessToken = accessToken

        // Enable FMParkingView internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        handleFMParkingViewDismiss()

        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            if(!hasRequestedEndRide){
                handleEndRideButton()
                hasRequestedEndRide = true
            }
        }

        return currentView
    }

    private fun openMap() {
        val bundle = Bundle()
        bundle.putDouble("latitude", lastResult.location.coordinate.latitude)
        bundle.putDouble("longitude", lastResult.location.coordinate.longitude)
        mapFragment = MapFragment()
        mapFragment.arguments = bundle
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.demo_fragment, mapFragment)
            .addToBackStack(null)
            .commit()
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
                resultsLayout.visibility = View.VISIBLE
                mapPinButton.visibility = View.GONE
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

        // Assign a controller
        fmParkingView.fmParkingViewController = fmParkingViewController

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        fmParkingView.isSimulation = isSimulationSwitch.isChecked

        // Enable Debug Mode to display session statistics
        fmParkingView.showStatistics = showStatisticsSwitch.isChecked

        // Present the FMParkingView to start
        fmParkingView.connect(sessionId)

        resultsLayout.visibility = View.GONE
        controlsLayout.visibility = View.GONE
    }

    private fun handleFMParkingViewDismiss() {
        fmParkingView.viewTreeObserver.addOnGlobalLayoutListener {
            if (fmParkingView.visibility == View.GONE) {
                //visibility has changed
                Log.d(TAG, "FMParkingView Changed Visibility")
                controlsLayout.visibility = View.VISIBLE
                hasRequestedEndRide = false
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
            override fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit) {
                Log.d(TAG, "QR Code Scan Successful")
                // Optional validation of the QR code can be done here
                // Note: If you choose to implement this method, you must call the `onValidQRCode` with the validation result
                // show dialogue to accept or refuse
                onValidQRCode(true)
            }

            override fun fmParkingView(result: FMLocationResult) {
                // Got a localization result
                // Localization will continue until you dismiss the view
                // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
                lastResult = result
                when (result.confidence) {
                    FMResultConfidence.HIGH -> {
                        Log.d(TAG, "HIGH Confidence Result")
                        fmParkingView.dismiss()
                        val stringResult =
                            "Result: ${result.location.coordinate} (${result.confidence})"
                        resultTextView.text = stringResult
                        if (resultsLayout.visibility == View.GONE) {
                            resultsLayout.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        Log.d(TAG, "${result.confidence} Confidence Result")
                    }
                }
            }

            override fun fmParkingView(error: ErrorResponse, metadata: Any?) {
                Log.e(TAG, "Received Error: $error")
            }
        }
}