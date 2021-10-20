package com.fantasmo.sdk.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.fantasmo.sdk.FMLocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class GoogleMapsView(val context: Context) : OnMapReadyCallback {

    private var anchorToggleButton: Boolean = false
    private var localizeMarkers: Queue<Marker> = LinkedList()
    private lateinit var anchor: Marker
    private lateinit var anchorRelativePosition: Marker

    lateinit var googleMapView: MapView
    private lateinit var googleMap: GoogleMap
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

    /**
     * Receives Coordinates from the server and
     * marks the mapView with the correspondent marker
     * Red - Localize or Update Anchor Methods
     * Blue - Anchor
     * @param latitude
     * @param longitude
     * */
    fun addCorrespondingMarkersToMap(result: FMLocationResult) {
        val latitude = result.location.coordinate.latitude
        val longitude = result.location.coordinate.longitude
        if (anchorToggleButton) {
            if (!this::anchor.isInitialized || anchor.tag == "AnchorDisabled") {
                addAnchorToMap(latitude, longitude)
                if (localizeMarkers.isNotEmpty()) {
                    localizeMarkers.forEach { it.remove() }
                }
            } else {
                updateAnchorRelativePosition(latitude, longitude)
            }
        } else {
            addLocalizeMarker(latitude, longitude)
        }
    }

    /**
     * Receives Coordinates from the server and marks the mapView with
     * a red marker meaning it's a localize request response from the server
     * Also zooms in the map according to the coordinates received
     * @param latitude
     * @param longitude
     * */
    private fun addLocalizeMarker(latitude: Double, longitude: Double) {
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(
                    LatLng(
                        latitude,
                        longitude
                    )
                )
                .title("Server Location")
        )
        localizeMarkers.add(marker)
        val latLong = LatLng(latitude, longitude)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 50F))

        if (localizeMarkers.size == 10) {
            val markerR = localizeMarkers.poll()
            markerR!!.remove()
        }
    }

    /**
     * When an anchor is set, it receives the latest coordinates from the server and marks
     * those coordinates in the mapView with a blue marker zooming in to that position
     * @param latitude
     * @param longitude
     * */
    private fun addAnchorToMap(latitude: Double, longitude: Double) {
        anchor = googleMap.addMarker(
            MarkerOptions()
                .position(
                    LatLng(
                        latitude,
                        longitude
                    )
                )
                .title("Anchor")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        )!!
        val latLong = LatLng(latitude, longitude)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 50F))
    }

    /**
     * When an anchor is set, it receives Coordinates from the server
     * and marks that position in the mapView with a red marker
     * @param latitude
     * @param longitude
     * */
    private fun updateAnchorRelativePosition(latitude: Double, longitude: Double) {
        if (this::anchorRelativePosition.isInitialized) {
            anchorRelativePosition.remove()
        }
        anchorRelativePosition = googleMap.addMarker(
            MarkerOptions()
                .position(
                    LatLng(
                        latitude,
                        longitude
                    )
                )
                .title("Position Relative to Anchor")
        )!!
    }

    fun unsetAnchor() {
        if (this::anchor.isInitialized) {
            anchor.remove()
            anchor.tag = "AnchorDisabled"
        }
        if (this::anchorRelativePosition.isInitialized) {
            anchorRelativePosition.remove()
        }
    }

    fun updateAnchor(anchorIsChecked: Boolean) {
        anchorToggleButton = anchorIsChecked
    }

    fun initGoogleMap(savedInstanceState: Bundle?) {
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap.isMyLocationEnabled = true
    }

    fun onSaveInstanceState(outState: Bundle){
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        googleMapView.onSaveInstanceState(mapViewBundle)
    }

    fun onStart() {
        googleMapView.onStart()
    }

    fun onDestroy(){
        googleMapView.onDestroy()
    }

    fun onStop() {
        googleMapView.onStop()
    }

    fun onResume() {
        googleMapView.onResume()
    }

    fun onPause() {
        googleMapView.onPause()
    }

    fun onLowMemory(){
        googleMapView.onLowMemory()
    }
}