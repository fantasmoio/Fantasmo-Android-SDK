# Fantasmo-Android
Demo application to demonstrate how to interact with and test the Fantasmo Android SDK.


## AAR Library
The SDK library is located inside the libs folder. When changing the SDK, the .aar should be replaced with the new version. 
  
## Dependencies

    // Include libs folder 
    implementation fileTree(dir: 'libs', include: ['*.aar'])
    
    // Google ARCore
    implementation 'com.google.ar:core:1.23.0'
    implementation 'com.google.ar.sceneform.ux:sceneform-ux:1.17.1'
    implementation 'com.google.ar.sceneform:core:1.17.1'
    implementation 'com.google.ar.sceneform:animation:1.17.1'
    implementation 'com.google.android.material:material:1.4.0-alpha01'

    // Location Services
    implementation 'com.google.gms:google-services:4.3.5'
    implementation 'com.google.android.gms:play-services-auth:19.0.0'
    implementation 'com.google.android.gms:play-services-location:18.0.0'

    //GSON for JSON parse and Volley for networking
    implementation 'com.google.code.gson:gson:2.8.6'
    implementation 'com.android.volley:volley:1.2.0'


## Permissions and requirements
ARCore compatibility is optional, so the minSdkVersion is 14 but in case the device does not support ARCore, the localize request will not work. The necessary permissions and feature are:
    
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />

## Schemes and Simulation Mode

Depending on the building flavor of the SDK when setting the flag isSimulation to true , GPS location and server URL will be overriden. Example of the values used when simulation is turned on for the devMunich flavour. You can find the aar files for the three available flavours on the folder "SimulationTestLibraryFiles" inside the folder libs. Replace the production aar with any of the flavours, in conjunction with isSimulation set to true to test with specific coordinates and url.

    devMunich {
            dimension "env"
            buildConfigField "String", "FM_API_BASE_URL", "\"https://api.fantasmo.io/v1/image.localize\""
            buildConfigField "String", "FM_GPS_LAT_LONG", "\"48.12863302178715,11.572371166069702\""
        }

You can test by using the pointing the camera to the images on the drawable folder, "image_on_street_munich" and "image_in_parking_paris".

## Setting up ARCore.

In the CameraFragment.kt there is an example of how to configures the necessary ARCore elements to have localization working. Firstly it's necessary to set up the ArFragment:

    arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
    arFragment.planeDiscoveryController.hide()
    arFragment.planeDiscoveryController.setInstructionView(null)
    arSceneView = arFragment.arSceneView
    
Then configuring the ARSession. 

    /**
     * Method to configure the AR Session, used to
     * enable auto focus for ARSceneView.
     */
    private fun configureARSession() {
        arSession = Session(context)
        val config = Config(arSession)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        arSession.configure(config)
        arSceneView.setupSession(arSession)
        Log.d(TAG, arSceneView.session?.config.toString())
    }

ARCore defaults it's resolution to 640x480. In order to improve image quality, the following piece of code should be added before the `arSession.configure(config)` line. This will find the largest resolution and use it during the ARSession. We've locked maximum resolution at 1080p resolution as we don’t recommend using more than that.

    var selectedSize = Size(0, 0)
    var selectedCameraConfig = 0

    val filter = CameraConfigFilter(arSession)
    val cameraConfigsList: List<CameraConfig> = arSession.getSupportedCameraConfigs(filter)
    for (currentCameraConfig in cameraConfigsList) {
        val cpuImageSize: Size = currentCameraConfig.imageSize
        val gpuTextureSize: Size = currentCameraConfig.textureSize

        if (cpuImageSize.width > selectedSize.width && cpuImageSize.height <= 1080) {
            selectedSize = cpuImageSize
            selectedCameraConfig = cameraConfigsList.indexOf(currentCameraConfig)
        }
    }
    arSession.cameraConfig = cameraConfigsList[selectedCameraConfig]
    

And set the onUpdateListener to localize the ARFrames.

    val scene = arSceneView.scene
    scene.addOnUpdateListener { frameTime ->
        run {
            arFragment.onUpdate(frameTime)
            onUpdate()
        }
    }
    

## Fantasmo SDK setup

First step is connecting the app with the SDK FMLocationManager. Example with the token and the listener to get the localization events.

    fmLocationManager.connect(
        "API_KEY",
        fmLocationListener
    )

Before starting to make use of the SDK features, the GPS location must be passed from the client app to the SDK and to have the best results, it should be kept updated:

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            currentLocation = locationResult.lastLocation
                
            //Set SDK Location
            fmLocationManager.setLocation(
                currentLocation.latitude,
                currentLocation.longitude
            )
            Log.d(TAG, "onLocationResult: ${locationResult.lastLocation}")
        }
    }

Then you can start or stop localizing using the following calls (done based on the 'Localize' toogle on the demo app). Note, to start localizing, you must provide an `appSessionId`, which is an identifier used for billing and tracking purposes:
    
    // Start getting location updates
    fmLocationManager.startUpdatingLocation(appSessionId: String)
    
    // Stop getting location updates
    fmLocationManager.stopUpdatingLocation()
    
Create the listener for location updates:
        
    /**
    * Listener for the Fantasmo SDK Location results.
    */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(result: FMLocationResult) {
            }
    }
    
    
And localize the ARFrames (done in the onUpdate on the sample app):

    // Localize current frame if not already localizing
    if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
        arFrame?.let { fmLocationManager.localize(it) }
    }

### Behaviors

To maximize localization quality, camera input is filtered against common problems. In order to enable camera input filtering, you should start localizing using the following call. By entering `true` value on `filtersEnabled` it will enable the behaviors described below. 

    // Start getting location updates
    fmLocationManager.startUpdatingLocation(appSessionId: String, filtersEnabled: Boolean)

The following listener will be called with behavior requests enabled and it's intended to alleviate such problems.

    private val fmLocationListener: FMLocationListener = {
        object : FMLocationListener {
            fun locationManager(didRequestBehavior: FMBehaviorRequest){
            }
        }
    }

The following behaviors are currently requested:

    enum class FMBehaviorRequest(val displayName: String) {
        TILTUP("Tilt your device up"),
        TILTDOWN("Tilt your device down"),
        PANAROUND("Pan around the scene"),
        PANSLOWLY("Pan more slowly"),
        ACCEPTED("Accepted");
    }

When notified, it should prompt the user to undertake the remedial behavior.

## Anchoring

Use the `Anchor` toggle to activate anchoring mode. The anchor position, i.e. the phone's position when anchoring is activated and sent to the SDK to be processed.
SDK methods to set and unset anchor: 

    // Set the anchor for the current frame
    val currentArFrame = arSceneView.arFrame
    currentArFrame?.let { fmLocationManager.setAnchor(it) }

    //Unset the current anchor
    fmLocationManager.unsetAnchor()

## ProGuard rules

The following rules should be added to the ProGuard file: 

    -dontwarn com.fantasmo.sdk.**
    -keep class com.fantasmo.sdk.** { *; }
    -keep class com.fantasmo.sdk.network.** { *; }the current frame

