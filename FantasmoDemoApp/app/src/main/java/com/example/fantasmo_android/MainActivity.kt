package com.example.fantasmo_android

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.example.fantasmo_android.helpers.PermissionsHelper
import com.google.ar.core.ArCoreApk


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
        checkPermissions()
        checkARCoreCompatibility()
    }


    private fun checkPermissions(){
        if (!PermissionsHelper.hasPermission(this)) {
            PermissionsHelper.requestPermission(this)
        }else{
            checkGPSEnabled()
        }
    }

    /**
     * After Requesting Permission if Camera and Location Permission are given, return main_activity_layout
     * and changes fragments according ARCore compatibility
     * @param requestCode: Request Code sent by requestPermission
     * @param permissions: Permissions to be given
     * @param grantResults: Result of permissions given (PERMISSION_GRANTED or PERMISSION_DENIED)
     * */
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
     * Checks Camera and Location Permissions
     * It's also responsible for changing fragments due to ARCore compatibility
     * */
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
     * Builds StartDestination on the navigation graph
     */
    private fun setStartDestination(){
        if (arcoreCompatibility) {
            //graph.startDestination = R.id.custom_arcore_fragment
            graph.startDestination = R.id.arcore_fragment
        } else {
            graph.startDestination = R.id.noarcore_fragment
        }
        navController.graph = graph
    }

    /**
     * Builds Alert Message to turn on GPS and redirects user to device settings
     */
    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationSettingsLauncher.launch(intent)
            }
            .setNegativeButton(
                "No"
            ) { dialog, _ -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private var locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        result->
        if(result.resultCode == Activity.RESULT_OK){
            val intent = result.data
            finish()
            startActivity(intent)
        }
    }

    /**
     * Checks if device is compatible with Google Play Services for AR
     */
    private fun checkARCoreCompatibility() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({
                checkARCoreCompatibility()
            }, 200)
        }
        if (availability.isSupported) {
            // The device is supported
            arcoreCompatibility = true
            Log.d(TAG, "ARCore is supported")
        } else {
            // The device is unsupported or unknown.
            arcoreCompatibility = false
            Log.d(TAG, "ARCore is not supported")
        }
    }
}