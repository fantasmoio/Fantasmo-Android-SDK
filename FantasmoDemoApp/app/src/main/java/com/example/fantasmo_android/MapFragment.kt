package com.example.fantasmo_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fantasmo_android.helpers.GoogleMapsManager
import com.google.android.gms.maps.MapView

class MapFragment : Fragment() {

    private lateinit var currentView: View
    private lateinit var googleMapView: MapView
    private lateinit var googleMapsManager: GoogleMapsManager
    private var lastResultLat = 0.0
    private var lastResultLng = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.map_view, container, false)
        googleMapView = currentView.findViewById(R.id.mapView)
        lastResultLat = requireArguments().getDouble("latitude")
        lastResultLng = requireArguments().getDouble("longitude")
        googleMapsManager = GoogleMapsManager(requireActivity(), googleMapView)
        googleMapsManager.setLastResult(lastResultLat, lastResultLng)
        googleMapsManager.initGoogleMap(savedInstanceState)

        return currentView
    }

    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        googleMapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        googleMapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        googleMapsManager.onSaveInstanceState(outState)
    }

    /**
     * Makes sure GoogleMaps View follows every Android lifecycle
     * https://developers.google.com/android/reference/com/google/android/gms/maps/MapView#developer-guide
     */
    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }

}