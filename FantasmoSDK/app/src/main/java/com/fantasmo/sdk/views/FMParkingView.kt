package com.fantasmo.sdk.views

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.fantasmosdk.R

class FMParkingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val TAG = "FMParkingView"
    private lateinit var arLayout: CoordinatorLayout
    private lateinit var googleMapsManager: GoogleMapsManager
    private lateinit var fmARCoreManager: FMARCoreManager
    var showStatistics = false
    var isSimulation = false
    var usesInternalLocationManager = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        setUpAREnvironmentOpenGL(inflater)
    }

    private fun setUpAREnvironmentOpenGL(inflater: LayoutInflater) {
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreManager = FMARCoreManager(arLayout, context)
        fmARCoreManager.setupARSession()
    }

    fun connect(accessToken: String, appSessionId: String) {
        googleMapsManager = GoogleMapsManager(context)
        fmARCoreManager.googleMapsManager = googleMapsManager
        fmARCoreManager.setupFantasmoEnvironment(
            accessToken,
            appSessionId,
            showStatistics,
            isSimulation,
            usesInternalLocationManager
        )
    }

    fun onResume() {
        fmARCoreManager.onResume()
        googleMapsManager.onResume()
    }

    fun onPause() {
        googleMapsManager.onPause()
        fmARCoreManager.onPause()
    }

    fun onDestroy() {
        fmARCoreManager.onDestroy()
        googleMapsManager.onDestroy()
    }

    fun onStart() {
        googleMapsManager.onStart()
    }

    fun onStop() {
        googleMapsManager.onStop()
    }

    fun onLowMemory() {
        googleMapsManager.onLowMemory()
    }

    fun updateLocation(latitude: Double, longitude: Double){
        fmARCoreManager.updateLocation(latitude,longitude)
    }

    fun initGoogleMap(savedInstanceState: Bundle?){
        googleMapsManager.initGoogleMap(savedInstanceState)
    }

    fun onSaveInstanceStateApp(outState: Bundle){
        googleMapsManager.onSaveInstanceState(outState)
    }
}