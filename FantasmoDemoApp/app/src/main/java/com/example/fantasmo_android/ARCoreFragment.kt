package com.example.fantasmo_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.views.FMParkingView
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.ar.core.*
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment(), OnMapReadyCallback
{
    private val TAG = "ARCoreFragment"

    private lateinit var currentView: View

    private lateinit var googleMapView: MapView
    private lateinit var googleMap: GoogleMap
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"


    private lateinit var fmParkingView: FMParkingView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        fmParkingView = currentView.findViewById(R.id.fmView)

        googleMapView = fmParkingView.getGoogleMapsView()
        initGoogleMap(savedInstanceState)

        return currentView
    }

    /**
     * To make any changes or get any values from ArFragment always call onResume lifecycle
     * */
    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
    }

    /**
     * Release heap allocation of the AR session
     * */
    override fun onDestroy() {
        fmParkingView.onDestroy()
        googleMapView.onDestroy()
        super.onDestroy()
    }

    /**
     * Initiates GoogleMap display on UI from savedInstanceState from onCreateView method
     * */
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        googleMapView.onCreate(mapViewBundle)
        googleMapView.getMapAsync(this)
    }


    override fun onResume() {
        super.onResume()
        fmParkingView.onResume()
        googleMapView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        googleMapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    override fun onPause() {
        googleMapView.onPause()
        super.onPause()
        fmParkingView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }

    override fun onMapReady(map: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap = map
        googleMap.isMyLocationEnabled = true
        fmParkingView.setGoogleMap(googleMap)
    }
}