package com.example.fantasmo_android

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    /**
     * Checks Camera and Location Permissions
     * */
    private fun checkPermissions() {
        val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
                    } != PackageManager.PERMISSION_GRANTED)) {
                this.let {
                    this.requestPermissions(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
                            1
                    )
                }
            } else {
                setContentView(R.layout.activity_main)
            }
        } else {
            buildAlertMessageNoGps()
        }
    }

    /**
     * After Requesting Permission if Camera and Location Permission are given, return main_activity_layout
     * @param requestCode: Request Code sent by requestPermission
     * @param permissions: Permissions to be given
     * @param grantResults: Result of permissions given (PERMISSION_GRANTED or PERMISSION_DENIED)
     * */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.size == 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setContentView(R.layout.activity_main)
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
}