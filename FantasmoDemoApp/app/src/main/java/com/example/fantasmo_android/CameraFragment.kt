package com.example.fantasmo_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment


class CameraFragment : Fragment() {

    private val TAG = "CameraFragment"

    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var currentView: View

    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var deviceCoorTv: TextView
    private lateinit var checkParkingButton: Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    private lateinit var locationManager: LocationManager
    private lateinit var fmLocationManager: FMLocationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.camera_fragment, container, false)

        cameraTranslationTv = currentView.findViewById(R.id.cameraTranslation)
        cameraAnglesTv = currentView.findViewById(R.id.cameraAnglesText)
        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)
        deviceCoorTv = currentView.findViewById(R.id.coordinatesText)
        localizeToggleButton = currentView.findViewById(R.id.localizeToggle)
        anchorToggleButton = currentView.findViewById(R.id.anchorToggle)

        fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        } else {
            checkLocationPermission()
        }
        return currentView
    }

    /**
     * To make any changes or get any values from ArFragment always call onResume lifecycle
     * */
    override fun onStart() {
        super.onStart()

        try {
            arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arSceneView = arFragment.arSceneView
            val scene = arSceneView.scene
            scene.addOnUpdateListener { frameTime ->
                run {
                    arFragment.onUpdate(frameTime)
                    onUpdate()
                }
            }

            // Connect the FMLocationManager from Fantasmo SDK
            fmLocationManager.connect(
                "8e785284ca284c01bd84116c0d18e8fd",
                fmLocationListener
            )

            checkParkingButton.setOnClickListener {
                // Add Check Parking Functionality
                Log.d("CameraFragment-> CheckPark Pressed", "CheckPark")
            }

            localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d("CameraFragment-> LocalizeToggle", "Enabled")

                    // Start getting location updates
                    fmLocationManager.startUpdatingLocation()
                } else {
                    Log.d("CameraFragment-> LocalizeToggle", "Disabled")

                    // Stop getting location updates
                    fmLocationManager.startUpdatingLocation()
                }
            }

            anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d("CameraFragment-> AnchorToggle", "Enabled")
                } else {
                    Log.d("CameraFragment-> AnchorToggle", "Disabled")
                }
            }

        } catch (e: Exception) {
            Log.d("CameraFragment-> ArFragment Null", "ArFragment")
        }
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                Log.d(TAG, error.message.toString())
            }

            override fun locationManager(location: Location, zones: List<FMZone>?) {
                Log.d(TAG, location.toString())
            }
        }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     * */
    private fun onUpdate() {
        val arFrame = arSceneView.arFrame

        val cameraTranslation = arFrame?.androidSensorPose?.translation
        cameraTranslationTv.text = createStringDisplay("Camera Translation: ", cameraTranslation)
        Log.d(
            "CameraFragment-> Camera Translation",
            createStringDisplay("Camera Translation: ", cameraTranslation)
        )

        val cameraRotation = arFrame?.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay("Camera Angles: ", cameraRotation)
        Log.d(
            "CameraFragment-> Camera Angles",
            createStringDisplay("Camera Angles: ", cameraRotation)
        )

        if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
            arFrame?.let { fmLocationManager.localize(it) }
        }
    }

    /**
     * Gets system location through the locationManager
     */
    private fun checkLocationPermission() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if ((activity?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED)
            ) {
                activity?.let { it ->
                    ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1
                    )
                }
            }
        } else {
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(
                "No"
            ) { dialog, _ -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun createStringDisplay(s: String, cameraAttr: FloatArray?): String {
        return s + String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " + String.format(
            "%.2f",
            cameraAttr?.get(2)
        )
    }
}