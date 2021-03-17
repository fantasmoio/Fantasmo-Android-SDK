package com.example.fantasmo_android.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
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
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import com.example.fantasmo_android.R
import com.example.fantasmo_android.utils.DemoAppUtils.AppUtils.createStringDisplay
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment


class CameraFragment: Fragment(), LocationListener {

    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var currentView: View

    private lateinit var cameraTranslationTv: TextView
    private lateinit var cameraAnglesTv: TextView
    private lateinit var deviceCoorTv : TextView
    private lateinit var checkParkingButton : Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton : Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton : Switch

    private lateinit var locationManager: LocationManager

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

        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }else{
            getLocation()
        }
        return currentView
    }

    /**
     * To make any changes or get any values from ArFragment always call onResume lifecycle
     * */
    override fun onResume(){
        super.onResume()

        try{
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

            checkParkingButton.setOnClickListener{
                Log.d("CameraFragment-> CheckPark Pressed", "CheckPark")
            }

            localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // if toggle button is enabled/on
                    Log.d("CameraFragment-> LocalizeToggle ", "Enabled")
                } else {
                    // If toggle button is disabled/off
                    Log.d("CameraFragment-> LocalizeToggle ", "Disabled")
                }
            }

            anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // if toggle button is enabled/on
                    Log.d("CameraFragment-> AnchorToggle ", "Enabled")
                } else {
                    // If toggle button is disabled/off
                    Log.d("CameraFragment-> AnchorToggle ", "Disabled")
                }
            }

        }catch (e: Exception){
            Log.d("CameraFragment-> ArFragment Null", "ArFragment")
        }
    }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     * */
    private fun onUpdate() {
        getLocation()
        val arFrame = arSceneView.arFrame

        val cameraTranslation = arFrame?.androidSensorPose?.translation
        cameraTranslationTv.text =
            createStringDisplay("Camera Translation: ", cameraTranslation)
        Log.d(
                "CameraFragment-> Camera Translation", createStringDisplay(
                "Camera Translation: ",
                cameraTranslation
        )
        )

        val cameraRotation = arFrame?.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text =
            createStringDisplay("Camera Angles: ", cameraRotation)
        Log.d(
                "CameraFragment-> Camera Angles", createStringDisplay(
                "Camera Angles: ",
                cameraRotation
        )
        )
    }

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     */
    private fun getLocation() {
        if ((context?.let {
                checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) &&
                (context?.let{
                    checkSelfPermission(
                            it,
                            Manifest.permission.CAMERA
                    )
                } != PackageManager.PERMISSION_GRANTED)) {
            if (shouldShowRequestPermissionRationale(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )) {
                this.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA),
                        1
                )
            } else {
                this.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA),
                        1
                )
            }
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location) {
        deviceCoorTv.text = "Device Lat: ${location.latitude} , Long: ${location.longitude}"
        Log.d(
                "CameraFragment-> LocationChanged: ",
                "New Latitude: ${location.latitude} and New Longitude: ${location.longitude}"
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d("CameraFragment-> OnRequestPermissionResult: Location", "Granted")
                    }
                    if ((ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                                    )) {
                        Log.d("CameraFragment-> OnRequestPermissionResult: Camera", "Granted")
                    }
                } else {
                    // Permission is not granted
                    activity?.finish()
                    Log.d("CameraFragment-> OnRequestPermissionResult: ", "Denied")
                }
                return
            }
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes"
            ) { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No"
            ) { dialog, _ -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
}