package com.example.fantasmo_android

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.example.fantasmo_android.helpers.PermissionsHelper
import com.example.fantasmo_android.helpers.SimulationUtils
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var arcoreCompatibility = true
    private lateinit var graph: NavGraph
    private lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        navHost =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!
        navController = navHost.navController

        val navInflater = navController.navInflater
        graph = navInflater.inflate(R.navigation.main_navigation)
        arcoreCompatibility = isARCoreSupportedAndUpToDate()
        checkPermissions()
    }

    /**
     * Checks Camera and Location Permissions
     *
     */
    private fun checkPermissions() {
        if (!PermissionsHelper.hasPermission(this)) {
            PermissionsHelper.requestPermission(this)
        } else {
            checkGPSEnabled()
        }
    }

    /**
     * After Requesting Permission if Camera and Location Permission are given, return main_activity_layout
     * and changes fragments according ARCore compatibility
     * @param requestCode Request Code sent by requestPermission
     * @param permissions Permissions to be given
     * @param grantResults Result of permissions given (PERMISSION_GRANTED or PERMISSION_DENIED)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!PermissionsHelper.hasPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(
                this,
                "Permission are needed to run this application",
                Toast.LENGTH_LONG
            )
                .show()
            if (!PermissionsHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                PermissionsHelper.launchPermissionSettings(this)
            }
            finish()
        } else {
            checkGPSEnabled()
        }
    }

    /**
     * Checks if GPS is turned on.
     * If GPS is turned off displays message to turn on.
     * Else delegates the fragment to display due to ARCore compatibility
     */
    private fun checkGPSEnabled() {
        val locationManager: LocationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setStartDestination()
        } else {
            buildAlertMessageNoGps()
        }
    }

    /**
     * Builds Alert Message to turn on GPS and redirects user to device settings
     */
    private fun buildAlertMessageNoGps() {
        val mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(300)
            .setFastestInterval(300)

        val settingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        settingsBuilder.setAlwaysShow(true)

        val result =
            LocationServices.getSettingsClient(this).checkLocationSettings(settingsBuilder.build())
        result.addOnCompleteListener { task ->
            //getting the status code from exception
            try {
                task.getResult(ApiException::class.java)
            } catch (ex: ApiException) {
                when (ex.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        val resolvableApiException = ex as ResolvableApiException
                        locationIntentLauncher.launch(IntentSenderRequest.Builder(resolvableApiException.resolution).build())
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG,
                            "PendingIntent unable to execute request."
                        )
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        Log.e(TAG,
                            "Something is wrong in your GPS"
                        )
                    }
                }
            }
        }
    }

    /**
     * Builds StartDestination on the navigation graph
     */
    private fun setStartDestination() {
        if (arcoreCompatibility) {
            if(SimulationUtils.useDemoFragment){
                graph.startDestination = R.id.arcore_fragment
            } else{
                graph.startDestination = R.id.custom_arcore_fragment
            }
        } else {
            graph.startDestination = R.id.noarcore_fragment
        }
        navController.graph = graph
    }

    private var locationIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()){
            result ->
            if(result.resultCode == Activity.RESULT_OK){
                setStartDestination()
            }
        }

    /**
     * Checks if device is compatible with Google Play Services for AR
     */
    private fun isARCoreSupportedAndUpToDate(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            Handler(Looper.getMainLooper()).postDelayed({
                isARCoreSupportedAndUpToDate()
            }, 200)
        }
        // Make sure ARCore is installed and supported on this device.
        when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {}
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> try {
                val installStatus = ArCoreApk.getInstance()
                    .requestInstall(this,  /*userRequestedInstall=*/true)
                // Request ARCore installation or update if needed.
                when (installStatus) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        Log.e(
                            TAG,
                            "ARCore installation requested."
                        )
                        return false
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {}
                    null -> {
                        return false
                    }
                }
            } catch (e: UnavailableException) {
                Log.e(
                    TAG,
                    "ARCore not installed",
                    e
                )
                return false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR,
            ArCoreApk.Availability.UNKNOWN_CHECKING,
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT,
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Log.e(
                    TAG,
                    "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned "
                            + availability
                )
                return false
            }
            null -> {
                return false
            }
        }
        return true
    }
}