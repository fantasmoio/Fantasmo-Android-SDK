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
    private lateinit var googleMapsView: GoogleMapsView
    private lateinit var fmARCoreView: FMARCoreView
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
        fmARCoreView = FMARCoreView(arLayout, context)
        fmARCoreView.setupARSession()
    }

    fun connect(accessToken: String, appSessionId: String) {
        googleMapsView = GoogleMapsView(context)
        fmARCoreView.googleMapsManager = googleMapsView
        fmARCoreView.setupFantasmoEnvironment(
            accessToken,
            appSessionId,
            showStatistics,
            isSimulation,
            usesInternalLocationManager
        )
    }

    fun onResume() {
        fmARCoreView.onResume()
        googleMapsView.onResume()
    }

    fun onPause() {
        googleMapsView.onPause()
        fmARCoreView.onPause()
    }

    fun onDestroy() {
        fmARCoreView.onDestroy()
        googleMapsView.onDestroy()
    }

    fun onStart() {
        googleMapsView.onStart()
    }

    fun onStop() {
        googleMapsView.onStop()
    }

    fun onLowMemory() {
        googleMapsView.onLowMemory()
    }

    fun updateLocation(latitude: Double, longitude: Double){
        fmARCoreView.updateLocation(latitude,longitude)
    }

    fun initGoogleMap(savedInstanceState: Bundle?){
        googleMapsView.initGoogleMap(savedInstanceState)
    }

    fun onSaveInstanceStateApp(outState: Bundle){
        googleMapsView.onSaveInstanceState(outState)
    }
}