package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
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
import com.example.fantasmo_android.helpers.GoogleMapsManager
import com.example.fantasmo_android.helpers.SimulationUtils
import com.example.fantasmo_android.helpers.SystemLocationListener
import com.example.fantasmo_android.helpers.SystemLocationManager

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMLocalizingViewProtocol
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMParkingViewProtocol
import com.fantasmo.sdk.views.FMQRScanningViewProtocol

import com.google.android.gms.maps.MapView
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class CustomDemoFragment : Fragment() {
    private val TAG = CustomDemoFragment::class.java.simpleName

    private lateinit var currentView: View

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var isSimulationSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showStatisticsSwitch: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button

    private lateinit var resultsLayout: ConstraintLayout
    private lateinit var resultTextView: TextView
    private lateinit var mapPinButton: Button
    private lateinit var skipButton: Button

    // Buttons and Views from the QRView
    private lateinit var fmQRView: ConstraintLayout
    private lateinit var qrCodeResultTv: TextView

    // Buttons and Views from the LocalizeView
    private lateinit var fmLocalizeView: ConstraintLayout
    private lateinit var filterRejectionTv: TextView
    private lateinit var mapButton: Button
    private lateinit var googleMapView: MapView
    private lateinit var googleMapsManager: GoogleMapsManager

    // Host App location Manager to exemplify how to set Location
    private lateinit var systemLocationManager: SystemLocationManager

    // Control variables for the FMParkingView
    private lateinit var fmParkingView: FMParkingView
    private val usesInternalLocationManager = true
    private val accessToken = SimulationUtils.API_KEY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.custom_demo_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)
        isSimulationSwitch = currentView.findViewById(R.id.simulationModeSwitch)
        showStatisticsSwitch = currentView.findViewById(R.id.showStatisticsSwitch)

        resultsLayout = currentView.findViewById(R.id.resultsLayout)
        resultTextView = currentView.findViewById(R.id.localizationResultView)
        mapPinButton = currentView.findViewById(R.id.mapPinButton)
        skipButton = currentView.findViewById(R.id.skipButton)
        skipButton.setOnClickListener{
            handleSkipQRScanning()
        }

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        // Assign an accessToken
        fmParkingView.accessToken = accessToken

        // Enable FMParkingView internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        handleQRView()
        handleLocalizeView(savedInstanceState)

        exitButton = currentView.findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            handleExitButton()
        }

        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            handleEndRideButton()
        }

        return currentView
    }

    private fun handleSkipQRScanning() {
        fmParkingView.skipQRScanning()
    }

    private fun handleEndRideButton() {
        // Test location of a parking space in Berlin
        val myLocation = getMyLocation()
        // Before trying to localize with Fantasmo you should check if the user is near a mapped parking space
        fmParkingView.isParkingAvailable(myLocation) {
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

    private fun getMyLocation(): Location {
        val location = Location("")
        if (isSimulationSwitch.isChecked) {
            location.latitude = SimulationUtils.latitude
            location.longitude = SimulationUtils.longitude
        }
        return location
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

        // Before presenting the FMParkingView register custom views, otherwise the default ones
        // will overpass these ones
        fmParkingView.registerQRScanningViewController(fmQrScanningViewController)
        fmParkingView.registerLocalizingViewController(fmLocalizingViewController)

        // Present the FMParkingView
        fmParkingView.connect(sessionId)

        useOwnLocationProvider()

        mapButton.visibility = View.VISIBLE
        controlsLayout.visibility = View.GONE
        exitButton.visibility = View.VISIBLE
    }

    private fun handleQRView() {
        qrCodeResultTv = currentView.findViewById(R.id.qrCodeResultTextView)
        fmQRView = currentView.findViewById(R.id.custom_qr_view) as ConstraintLayout
    }

    private fun handleLocalizeView(savedInstanceState: Bundle?) {
        fmLocalizeView = currentView.findViewById(R.id.custom_localize_view)
        googleMapView = currentView.findViewById(R.id.mapView)
        googleMapsManager = GoogleMapsManager(requireActivity(), googleMapView)
        googleMapsManager.initGoogleMap(savedInstanceState)

        filterRejectionTv = currentView.findViewById(R.id.custom_filterRejectionTextView)
        mapButton = currentView.findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            if (googleMapView.visibility == View.VISIBLE) {
                googleMapView.visibility = View.GONE
            } else {
                googleMapView.visibility = View.VISIBLE
            }
        }
    }

    private fun handleExitButton() {
        fmParkingView.dismiss()
        googleMapsManager.unsetAnchor()

        exitButton.visibility = View.GONE
        controlsLayout.visibility = View.VISIBLE
        fmLocalizeView.visibility = View.GONE
    }

    /**
     * Android lifecycle methods
     */
    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
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
        googleMapView.onResume()
    }

    /**
     * Stops the AR session
     */
    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    /**
     * Pauses the AR session
     */
    override fun onPause() {
        super.onPause()
        fmParkingView.onPause()
        googleMapView.onPause()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        googleMapsManager.onSaveInstanceState(outState)
    }

    /**
     * Makes sure GoogleMaps View follows every Android lifecycle
     * https://developers.google.com/android/reference/com/google/android/gms/maps/MapView#developer-guide
     */
    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }

    /**
     * Example on how to override the internal location Manager
     */
    private fun useOwnLocationProvider() {
        if (!usesInternalLocationManager) {
            systemLocationManager = SystemLocationManager(context, systemLocationListener)
        }
    }

    private val systemLocationListener: SystemLocationListener =
        object : SystemLocationListener {
            override fun onLocationUpdate(currentLocation: Location) {
                fmParkingView.updateLocation(
                    currentLocation
                )
            }
        }

    /**
     * Listener for the FMParkingView.
     */
    private val fmParkingViewController: FMParkingViewProtocol =
        object : FMParkingViewProtocol {
            override fun fmParkingViewDidStartQRScanning() {
                Log.d(TAG, "fmParkingViewDidStartQRScanning")
            }

            override fun fmParkingViewDidStopQRScanning() {
                Log.d(TAG, "fmParkingViewDidStopQRScanning")
            }

            override fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit) {
                Log.d(TAG, "fmParkingView ShouldContinue")
                // Optional validation of the QR code can be done here
                // Note: If you choose to implement this method, you must call the `shouldApprove` with the validation result
                // show dialogue to accept or refuse
                val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
                builder1.setTitle("QR Code Scan result")
                builder1.setMessage(qrCode)
                builder1.setCancelable(true)

                builder1.setPositiveButton(
                    "Yes"
                ) { dialog, _ ->
                    dialog.cancel()
                    onValidQRCode(true)
                    Log.d(TAG, "QR Code Accepted")
                }

                builder1.setNegativeButton(
                    "No"
                ) { dialog, _ ->
                    dialog.cancel()
                    onValidQRCode(false)
                    Log.d(TAG, "QR Code Refused")
                }

                val alert11: AlertDialog = builder1.create()
                alert11.show()

            }

            override fun fmParkingViewDidStartLocalizing() {
                Log.d(TAG, "fmParkingViewDidStartLocalizing")
                if (fmLocalizeView.visibility == View.GONE) {
                    fmLocalizeView.visibility = View.VISIBLE
                }
            }

            override fun fmParkingView(behavior: FMBehaviorRequest) {
                Log.d(TAG, "fmParkingView Behavior")
            }

            override fun fmParkingView(result: FMLocationResult) {
                Log.d(TAG, "fmParkingView Result")
                // Got a localization result
                // Localization will continue until you dismiss the view
                // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
                if (result.confidence == FMResultConfidence.LOW) {
                    Log.d(TAG, "Low Confidence Result")
                }
                googleMapsManager.addCorrespondingMarkersToMap(result)
            }

            override fun fmParkingView(error: ErrorResponse, metadata: Any?) {}
        }

    /**
     * Custom Listener for the Fantasmo Localizing View.
     */
    private var fmLocalizingViewController: FMLocalizingViewProtocol =
        object : FMLocalizingViewProtocol {
            override fun didStartLocalizing() {
                Log.d(TAG, "didStartLocalizing")
            }

            override fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest) {
                Log.d(TAG, "didRequestLocalizationBehavior")
                val stringResult = behavior.description
                filterRejectionTv.text = stringResult
                if (filterRejectionTv.visibility == View.GONE) {
                    filterRejectionTv.visibility = View.VISIBLE
                    val timer = object : CountDownTimer(2000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {}
                        override fun onFinish() {
                            filterRejectionTv.visibility = View.GONE
                        }
                    }.start()
                }
            }

            override fun didReceiveLocalizationResult(result: FMLocationResult) {
                Log.d(TAG, "didReceiveLocalizationResult")
                if (result.confidence == FMResultConfidence.HIGH) {
                    val stringResult =
                        "Result: ${result.location.coordinate} (${result.confidence})"
                    resultTextView.text = stringResult
                    if (resultsLayout.visibility == View.GONE) {
                        resultsLayout.visibility = View.VISIBLE
                    }
                }
            }

            override fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?) {
                Log.d(TAG, "didReceiveLocalizationError")
            }
        }

    /**
     * Custom Listener for the Fantasmo QR Scanning View.
     */
    private var fmQrScanningViewController: FMQRScanningViewProtocol =
        object : FMQRScanningViewProtocol {
            override fun didStartQRScanning() {
                Log.d(TAG, "didStartQRScanning")
                googleMapsManager.updateAnchor(true)

                if (fmQRView.visibility == View.GONE) {
                    fmQRView.visibility = View.VISIBLE
                }
            }

            override fun didScanQRCode(result: String) {
                Log.d(TAG, "didScanQRCode")
                qrCodeResultTv.text = result
                if (fmQRView.visibility == View.VISIBLE) {
                    fmQRView.visibility = View.GONE
                }
            }

            override fun didStopQRScanning() {
                Log.d(TAG, "didStopQRScanning")
                if (qrCodeResultTv.visibility == View.VISIBLE) {
                    qrCodeResultTv.visibility = View.GONE
                }
                if (fmQRView.visibility == View.VISIBLE) {
                    fmQRView.visibility = View.GONE
                }
            }
        }
}