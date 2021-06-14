# Fantasmo-Android-SDK

## Overview

Supercharge your app with hyper-accurate positioning using just the camera. The Fantasmo SDK is the gateway to the Camera Positiong System (CPS) which provides 6 Degrees-of-Freedom (position and orientation) localization for mobile devices.

## Installation

Add the .aar library file to your app and make sure the folder it's added in the dependencies of the app.
  
### Dependencies

    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8'
    implementation 'com.android.volley:volley:1.2.0'
    implementation 'com.google.code.gson:gson:2.8.6'
    implementation 'com.google.ar:core:1.23.0'
    implementation 'com.google.ar.sceneform.ux:sceneform-ux:1.17.1'
    implementation 'com.google.android.gms:play-services-location:18.0.0'

### Building and Importing

To build the library .aar, the desired Build Variant should be seleted and then build the project. The .aar will be located in /app/build/outputs/aar/
The Fantasmo SDK .aar file can be imported directly into a project.

## Requirements

- Android version 7.0+
- Android Studio 4.1.2+

## Functionality

### Localization

Camera-based localization is the process of determining the global position of the device camera from an image. Image frames are received from the client app and sent to a server for computation. The server computation time is approximately 900 ms. The full round trip time is then dictated by latency of the connection.

Since the camera will likely move after the moment at which the image frame is captured, it is necessary to track the motion of the device continuously during localizaiton to determine the position of the device at the time of response. Tracking is provided by `ARSession`. Conventiently, it is then possible to determine the global position of the device at any point in the tracking session regardless of when the image was captured (though you may incur some drift after excessive motion).

### Anchors

Localization determines the position of the camera at a point in time. If it is desired to track the location of an object besides the camera itself (e.g., a scooter), then it is possible to set an anchor point for that object. When an anchor is set, the location update will provide the location of the anchor instead of the camera. The anchor position is determined by applying the inverse motion since the anchor was set until the localization was request was made. 

### Semantic Zones

The utility of precise localization is only as useful as the precision of the underlying map data. Semantic zones (e.g., "micro-geofences") allow your application to make contextual decisions about operating in the environment. 

When a position is found that is in a semantic zone, the server will report the zone type and ID. The zone types are as follows:

+ Street
+ Sidewalk
+ Furniture
+ Crosswalk
+ Access Ramp
+ Mobility parking
+ Auto parking
+ Bus stop
+ Planter


## Usage

### Quick Start 

Try out the `FantasmoDemoApp` project or implement the code below. 

    fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!
    
    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                // Handle error
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(location: Location, zones: List<FMZone>?) {
                // Handle location update
            }
        }
    
    // Connect the FMLocationManager from Fantasmo SDK
    fmLocationManager.connect(
        "API_KEY",
        fmLocationListener
    )

    // Start getting location updates
    fmLocationManager.startUpdatingLocation()
    
### Initialization

The location manager is accessed through a initialized instance.  

    var fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!
    
    // Connect the FMLocationManager from Fantasmo SDK
    fmLocationManager.connect(
        "API_KEY",
        fmLocationListener
    )
    
### Localizing 

To have location updates the client app must update the device GPS coordinates for the SDK to use. It should be done using the following call:

    fun setLocation(latitude: Double, longitude: Double)
    
To start location updates, there are two options. The first being without any Image frame filtration, which makes Image frames being continuously captured and sent to the server for localization:

    fmLocationManager.startUpdatingLocation() 

The second one, makes each Image frame pass a filter in order to reduce the amount of requests into the server. These filter include discarding Image frames that are blurry, Image frames that have the same position as previous ones and Image frames that have high and low pitch values:

    fmLocationManager.startUpdatingLocation(true) 

To stop location updates:

    fmLocationManager.stopUpdatingLocation()

Location events are be provided through `FMLocationListener`.

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                // Handle error
            }
            override fun locationManager(location: Location, zones: List<FMZone>?) {
                // Handle location update
            }
        }

### Anchors

In order to get location updates for an anchor, set the anchor before
starting or during location updates. 

    val currentArFrame = arSceneView.arFrame
    currentArFrame?.let { fmLocationManager.setAnchor(it) }

To return to device localization, simply unset the anchor point. 

    fmLocationManager.unsetAnchor()

### Simulation Mode

Since it's not always possible to be onsite for testing, a simulation mode is provided
queries the localization service with stored images. 

In order to activate simulation mode, set the flag and choose a semantic zone type to simulate. 

    fmLocationManager.isSimulation = true
    
### Overrides

For testing, the device location and server URL can be specified in the different flavours of the SDK

    buildConfigField "String", "FM_API_BASE_URL", "\"https://api.fantasmo.io/v1/image.localize\""
    
    buildConfigField "String", "FM_GPS_LAT_LONG", "\"48.848138681935886,2.371750713292894\""
