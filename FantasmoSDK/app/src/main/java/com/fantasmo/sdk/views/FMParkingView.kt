package com.fantasmo.sdk.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMARCoreManager2
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

    private lateinit var fmARCoreManager2: FMARCoreManager2

    private fun setUpAREnvironmentOpenGL(inflater: LayoutInflater) {
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreManager2 = FMARCoreManager2(arLayout, context)
        googleMapsManager = GoogleMapsManager()
        fmARCoreManager2.googleMapsManager = googleMapsManager
        fmARCoreManager2.setupARSession()
    }

    fun setGoogleMap(googleMap: GoogleMap){
        googleMapsManager.googleMap = googleMap
    }

    fun getGoogleMapsView(): MapView {
        return fmARCoreManager2.googleMapView
    }

    fun onResume() {
        fmARCoreManager2.onResume()
    }

    fun onPause() {
        fmARCoreManager2.onPause()
    }

    fun onDestroy(){
        fmARCoreManager2.onDestroy()
    }
}