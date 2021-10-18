package com.fantasmo.sdk.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMARCoreManager
import com.fantasmo.sdk.fantasmosdk.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

class FMParkingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val TAG = "FMParkingView"
    private lateinit var arLayout: CoordinatorLayout
    private lateinit var googleMapsManager: GoogleMapsManager

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        setUpAREnvironmentOpenGL(inflater)

    }

    private lateinit var fmARCoreManager: FMARCoreManager

    private fun setUpAREnvironmentOpenGL(inflater: LayoutInflater) {
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreManager = FMARCoreManager(arLayout, context)
        googleMapsManager = GoogleMapsManager()
        fmARCoreManager.googleMapsManager = googleMapsManager
        fmARCoreManager.setupARSession()
    }

    fun fmConnectToAPI(accessToken: String){
        fmARCoreManager.setupFantasmoEnvironment(accessToken)
    }

    fun setGoogleMap(googleMap: GoogleMap){
        googleMapsManager.googleMap = googleMap
    }

    fun getGoogleMapsView(): MapView {
        return fmARCoreManager.googleMapView
    }

    fun onResume() {
        fmARCoreManager.onResume()
    }

    fun onPause() {
        fmARCoreManager.onPause()
    }

    fun onDestroy(){
        fmARCoreManager.onDestroy()
    }
}