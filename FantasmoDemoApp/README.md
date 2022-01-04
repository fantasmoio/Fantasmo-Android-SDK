# Fantasmo-Android
Demo application to demonstrate how to interact with and test the Fantasmo Android SDK.


## AAR Library
The SDK library is located inside the libs folder. When changing the SDK, the .aar should be replaced with the new version. 
  
## Dependencies
```kotlin
// Include libs folder 
implementation fileTree(dir: 'libs', include: ['*.aar'])

implementation 'androidx.core:core-ktx:1.3.2'
implementation 'androidx.appcompat:appcompat:1.3.1'
implementation 'com.google.android.material:material:1.4.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.1'
implementation 'androidx.legacy:legacy-support-v4:1.0.0'

// Fragment Navigation
implementation "androidx.navigation:navigation-fragment-ktx:2.3.5"

// Google ARCore
implementation 'com.google.ar:core:1.29.0'

// Location Services
implementation 'com.google.android.gms:play-services-location:18.0.0'
implementation 'com.google.android.gms:play-services-maps:17.0.1'

// Barcode model dependencies
implementation 'com.google.mlkit:barcode-scanning:17.0.0'

//GSON for JSON parse and Volley for networking
implementation 'com.google.code.gson:gson:2.8.6'
implementation 'com.android.volley:volley:1.2.0'

// TensorFlow Lite
implementation 'org.tensorflow:tensorflow-lite-support:0.1.0'
implementation 'org.tensorflow:tensorflow-lite-metadata:0.1.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.3.0'
```

## Building

On the module-level `build.gradle`, inside the `android` properties you should add the following instruction. This will allow to add a machine learning model and to loaded it when in a Localizing Session.
```kotlin
    aaptOptions {
        noCompress "tflite"
        noCompress "lite"
    }
```

## Permissions and requirements
ARCore compatibility is optional and the `minSdkVersion` is **16**. In case the device does not support ARCore, the FMParkingView will not work. The necessary permissions and feature are:

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```
## Schemes and Simulation Mode

Depending on the building flavor of the SDK when setting the flag `isSimulation` to `true`, GPS location and server URL will be overriden. Example of the values used when simulation is turned on for the `devMunich` flavour. You can find the `.aar` files for the three available flavours on the folder `"SimulationTestLibraryFiles"` inside the folder `libs`. Replace the production `.aar` with any of the flavours, in conjunction with `isSimulation` set to true to test with specific coordinates and url.

```kotlin
devMunich {
    dimension "env"
    buildConfigField "String", "FM_API_BASE_URL", "\"https://api.fantasmo.io/v1/image.localize\""
    buildConfigField "String", "FM_GPS_LAT_LONG", "\"48.12863302178715,11.572371166069702\""
}
```

You can test by using the pointing the camera to the images on the drawable folder, `"image_on_street.jpg"` and `"image_in_parking.jpg"`.

## Setting up FMParkingView

In the `DemoFragment.kt` there is an example of how to set up the FMParkingView to start a parking session.
Firstly, like the `demo_fragment.xml` demonstrates, it's necessary to create the FMParkingView:
```xml
<com.fantasmo.sdk.views.FMParkingView
    android:id="@+id/fmParkingView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">
</com.fantasmo.sdk.views.FMParkingView>
```
After that, to initialize it we only need to do `findViewById` or equivalent and call it on the `onCreate` lifecycle. After this it should provide a camera preview displaying the current `ARSession`.

## Parking Flow

Before attempting to park and localize with Fantasmo SDK, you should first check if parking is available in the user's current location. You can do this by replicating the following method `fmParkingView.isParkingAvailable(location: Location, onCompletion:(Boolean) → Unit)` passing a latitude and longitude of the location. The result block is called with a boolean indicating whether or not the user is near a mapped parking space.
```kotlin
fmParkingView.isParkingAvailable(location) { isParkingAvailable: Boolean
    if (isParkingAvailable) {
        // Create and present FMParkingView here
    } else {
        Log.e(TAG,"No mapped parking spaces nearby.")
    }
}
```    
**Important:** Before atempting parking please make sure you provide the FMParkingView with an accessToken, otherwise it will deny your access to the Fantasmo SDK features. (e.g. `fmParkingView.accessToken = "API_KEY"`)

After this, we are ready to connect to the Fantasmo SDK. We need to provide a controller to get the results from the SDK and a sessionId. Here's an example of connecting to the FMParkingView and start a parking session: 

```kotlin
fmParkingView.fmParkingViewController = fmParkingViewController
val sessionId = UUID.randomUUID().toString()
fmParkingView.connect(sessionId)
```

The SDK provides an internal LocationManager and it will give updates on location. If you want to use your own Location Manager, all you have to do is set `fmParkingView.usesInternalLocationManager` to false and call the `fmParkingView.updateLocation(location: Location)` on your location manager in order to get location updates. If you check `CustomDemoFragment.kt` there's an example of how to manage your own location updates: 

```kotlin
// Custom Location Manager
private val systemLocationListener: SystemLocationListener =
    object : SystemLocationListener {
        override fun onLocationUpdate(currentLocation: Location) {
            fmParkingView.updateLocation(currentLocation)
        }
    }
```
Create the listener for the result updates:
```kotlin        
/**
 * Listener for the FMParkingView.
 */
private val fmParkingViewController: FMParkingViewProtocol =
    object : FMParkingViewProtocol {
        override fun fmParkingViewDidStartQRScanning() {
        }

        override fun fmParkingViewDidStopQRScanning() {
        }

        override fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit) {
            // Optional validation of the QR code can be done here
            // Note: If you choose to implement this method, you must call the `onValidQRCode` with the validation 
            // result show dialogue to accept or refuse
            onValidQRCode(true)
        }

        override fun fmParkingViewDidStartLocalizing() {
        }

        override fun fmParkingView(behavior: FMBehaviorRequest) {
        }

        override fun fmParkingView(result: FMLocationResult) {
            // Got a localization result
            // Localization will continue until you dismiss the view
            // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
        }

        override fun fmParkingView(error: ErrorResponse, metadata: Any?) {
        }
    }
```

If a QR code cannot be scanned and/or you've collected the necessary info from the user manually, then you may skip this step and proceed directly to localization.
```kotlin
private fun handleSkipQRScanning() {
    fmParkingView.skipQRScanning()
}
```
**Note:** During a QR code scanning session, it is not possible to turn on the flashlight due to ARCore being used on the FMParkingView. ARCore blocks any input regarding turning on/off the flashlight during an AR session, limiting QR code readability on dark environments.

### Customizing UI

The SDK, provides with default views for both the QRScanning and Localizing views. If you want to customize these views, you need to provide with your own view controllers. We provide an example in the `CustomDemoFragment.kt` with the following controllers filled with view management.

```kotlin
private var fmQrScanningViewController: FMQRScanningViewProtocol =
    object : FMQRScanningViewProtocol {
        override fun didStartQRScanning() {}
        override fun didScanQRCode(result: String) {}
        override fun didStopQRScanning() {}
    }

private var fmLocalizingViewController: FMLocalizingViewProtocol =
    object : FMLocalizingViewProtocol {
        override fun didStartLocalizing() {}
        override fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest) {}
        override fun didReceiveLocalizationResult(result: FMLocationResult) {}
        override fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?) {}
    }
```

Once you've created view controllers for the above protocols, simply register them with your `FMParkingView` instance before presenting it, otherwise the default ones will overpass these ones.
```kotlin
    fmParkingView.registerQRScanningViewController(fmQrScanningViewController)
    fmParkingView.registerLocalizingViewController(fmLocalizingViewController)
```
## ProGuard rules

The following rules should be added to the ProGuard file: 

    -dontwarn com.fantasmo.sdk.**
    -keep class com.fantasmo.sdk.** { *; }
    -keep class com.fantasmo.sdk.network.** { *; }the current frame

