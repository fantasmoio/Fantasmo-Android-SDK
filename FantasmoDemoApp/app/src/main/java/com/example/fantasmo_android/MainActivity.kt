package com.example.fantasmo_android

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
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
        checkARCoreCompatibility()
    }

    /**
     * Checks Camera and Location Permissions
     * It's also responsible for changing fragments due to ARCore compatibility
     * */
    private fun checkPermissions() {
        val locationManager: LocationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if ((this.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.CAMERA
                    )
                } != PackageManager.PERMISSION_GRANTED) &&
                (this.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED) &&
                (this.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } != PackageManager.PERMISSION_GRANTED)) {
                this.let {
                    this.requestPermissions(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        1
                    )
                }
            } else {
                if (arcoreCompatibility) {
                    //graph.startDestination = R.id.custom_arcore_fragment
                    graph.startDestination = R.id.arcore_fragment
                } else {
                    graph.startDestination = R.id.noarcore_fragment
                }
                navController.graph = graph
            }
        } else {
            buildAlertMessageNoGps()
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
        if (requestCode == 1) {
            if (grantResults.size == 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                if (arcoreCompatibility) {
                    //graph.startDestination = R.id.custom_arcore_fragment
                    graph.startDestination = R.id.arcore_fragment
                } else {
                    graph.startDestination = R.id.noarcore_fragment
                }
                navController.graph = graph
            }
        }
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
                startActivityForResult(intent, 2)
            }
            .setNegativeButton(
                "No"
            ) { dialog, _ -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    /**
     * After the user has been directed to Settings, restart Activity
     * @param requestCode: Request sent by startActivityForResult
     * @param resultCode: Result after activity started
     * @param data: Intent of the performed activity
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            2 -> {
                if (resultCode == 0) {
                    finish()
                    startActivity(intent)
                }
            }
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
        checkPermissions()
    }
}