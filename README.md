# Fantasmo-Android-SDK

## Overview

Supercharge your app with hyper-accurate positioning using just the camera. The Fantasmo SDK is the gateway to the Camera Positioning System (CPS) which provides 6 Degrees-of-Freedom (position and orientation) localization for mobile devices.

## Installation

Add the .aar library file to your app and make sure the folder it's added in the dependencies of the app.
  
### Dependencies
```kotlin
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8'
implementation 'com.android.volley:volley:1.2.0'
implementation 'com.google.code.gson:gson:2.8.6'
implementation 'androidx.core:core-ktx:1.3.2'

// ARCore
implementation 'com.google.ar:core:1.29.0'

// Location Services
implementation 'com.google.android.gms:play-services-location:18.0.0'

// Barcode model dependencies
implementation 'com.google.mlkit:barcode-scanning:17.0.0'

// Layouts
implementation 'androidx.appcompat:appcompat:1.3.1'
implementation 'com.google.android.material:material:1.4.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.1'
implementation 'androidx.coordinatorlayout:coordinatorlayout:1.1.0'

// TensorFlow Lite
implementation 'org.tensorflow:tensorflow-lite-support:0.1.0'
implementation 'org.tensorflow:tensorflow-lite-metadata:0.1.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.3.0'
```
### Building and Importing

On the module-level `build.gradle`, inside the `android` properties you should add the following instruction. This will allow to add a machine learning model and to loaded it when in a Localizing Session.
```kotlin
    aaptOptions {
        noCompress "tflite"
        noCompress "lite"
    }
```

To build the library .aar, the desired Build Variant should be seleted and then build the project. The .aar will be located in /app/build/outputs/aar/
The Fantasmo SDK .aar file can be imported directly into a project.

## Requirements

- Android version 4.1+
- Android Studio 4.1.2+

## Functionality

Camera-based localization is the process of determining the global position of the device camera from an image. Image frames are acquired from an active `ARSession` and sent to a server for computation. The server computation time is approximately 900 ms. The full round trip time is then dictated by latency of the connection.

Since the camera will likely move after the moment at which the image frame is captured, it is necessary to track the motion of the device continuously during localizaiton to determine the position of the device at the time of response. Tracking is provided by `ARSession`. Conveniently, it is then possible to determine the global position of the device at any point in the tracking session regardless of when the image was captured (though you may incur some drift after excessive motion).

## Usage

### Quick Start 

Try out the `FantasmoDemoApp` project or implement the code below. 
```kotlin 
/**
 * Listener for the Fantasmo SDK Location results.
 */
private val fmParkingViewController: FMParkingViewProtocol =
    object : FMParkingViewProtocol {
        override fun fmParkingView(qrCode: Barcode, continueBlock: (Boolean) -> Unit) {
            // Handle QR Code result
        }
        
        override fun fmParkingView(qrCode: String, shouldContinue: (Boolean) -> Unit) {
            // Handle Manual QR Code result
        }

        override fun fmParkingView(behavior: FMBehaviorRequest) {
            // Handle Behavior Request
        }

        override fun fmParkingView(result: FMLocationResult) {
            // Handle localization result
        }

        override fun fmParkingView(error: ErrorResponse, metadata: Any?) {
            // Handle error
        }
    }

fmParkingView = findViewById(R.id.fmParkingView)

// Connect the FMParkingView Controller to FMParkingView
fmParkingView.fmParkingViewController = fmParkingViewController

// Assign an accessToken to the FMParkingView
fmParkingView.accessToken = "API_KEY"

// Present FMParkingView with a sessionId
val sessionId = UUID.randomUUID().toString()
fmParkingView.connect(sessionId)
```

And add this to your `layout.xml` file:
```xml
<com.fantasmo.sdk.views.FMParkingView
    android:id="@+id/fmParkingView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
</com.fantasmo.sdk.views.FMParkingView>
```
### Checking Availability

Before attempting to park and localize with Fantasmo SDK, you should first check if parking is available in the user's current location. You can do this with the method `fmParkingView.isParkingAvailable(location: Location, onCompletion:(Boolean) → Unit)` passing an Android [Location](https://developer.android.com/reference/kotlin/android/location/Location) object. The result block is called with a boolean indicating whether or not the user is near a mapped parking space.
```kotlin
fmParkingView.isParkingAvailable(location) { isParkingAvailable: Boolean
    if (isParkingAvailable) {
        // Create and present FMParkingView here
    } else {
        Log.e(TAG,"No mapped parking spaces nearby.")
    }
}
```
### Providing a `sessionId`

The `sessionId` parameter allows you to associate localization results with your own session identifier. Typically this would be a UUID string, but it can also follow your own format. For example, a scooter parking session might take multiple localization attempts. For analytics and billing purposes, this identifier allows you to link multiple attempts with a single parking session.

### Initialization

The FMParkingView is initialized as long as you import it to your `layout.xml` file: 
```xml
<com.fantasmo.sdk.views.FMParkingView
    android:id="@+id/fmParkingView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
</com.fantasmo.sdk.views.FMParkingView>
```

After that, when you inflate the layout of your App, initialize the FMParkingView with `findViewById` and call with it's registered id (e.g. `fmParkingView = yourView.findViewById(R.id.fmParkingView)`). After that, it should be ready to present a camera preview when you run the App.

**Important:** Since the `FMParkingView` is ARCore based, you should call the `fmParkingView` throughout your app lifecycle. We provide `onResume()`, `onPause()` and `onDestroy()` methods and you should invoke them by calling, for example, `fmParkingView.onResume()` on your App's `onResume()` method.


### Providing Location Updates

By default, during localization the `FMParkingView` uses a `LocationManager` internally to get automatic updates of the device's location. If you would like to provide your own location updates, you can set the `usesInternalLocationManager` property to false and manually call `updateLocation(location: Location)` with each update to the location.
```kotlin
fmParkingView.connect(sessionId)
fmParkingView.usesInternalLocationManager = false

// create your own Location Manager
val locationRequest = LocationRequest.create()
locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
locationRequest.smallestDisplacement = 1f
locationRequest.fastestInterval = locationInterval
locationRequest.interval = locationInterval

val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: Location) {
        // notify the FMParkingView of the location update
        fmParkingView.updateLocation(locationResult)      
    }
}

fusedLocationClient.requestLocationUpdates(
    locationRequest,
    locationCallback,
    Looper.myLooper()!!
)
```

If an error occurs you should check that you're correctly providing the location updates, or if you're using the internal location manager, that the user has given permission to access the device's location.

### QR Codes

Scanning a QR code is the first and only step before localizing. Because we are trying to localize a vehicle and not the device itself, we need a way to determine the vehicle's position relative to the device. This is accomplished by setting an anchor in the `ARSession` and it's done automatically when the user scans a QR code. 

The SDK doesn't care about the contents of the QR code and by default will start localizing after any QR code is detected. If your app does care about the contents of the QR code, they can be validated by implementing the `FMParkingViewController` method:
```kotlin
override fun fmParkingView(qrCode: Barcode, continueBlock: (Boolean) -> Unit) {
    val validQRCode = qrCode.rawValue!=null
    // Validation of the QR code can be done here
    continueBlock(validQRCode)
}
```

### Manual QR Code Entry
If a code is unable to be scanned, you may want to have the user enter it manually. When using the default QR code scanning UI, this feature is implemented for you. Simply tap the *Enter Manually* button and enter the code into the prompt. If you are using a custom UI, then you should prompt the user to enter the code and pass the string to the `enterQRCode(qrCodeString: String)` method of your parking view controller.

Validating a manually-entered QR code is also optional and works the same as a validating a scanned one. Implement the following method in your `FMParkingViewController`.
```kotlin
override fun fmParkingView(qrCodeString: String, continueBlock: (Boolean) -> Unit) {
    val validQRCode = qrCodeString.isNotEmpty()
    // Validation of the QR code can be done here
    continueBlock(validQRCode)

}
```

**Important:** If you implement this method, you must call the `continueBlock` with a boolean value. A value of `true` indicates the QR code is valid and that localization should start. Passing `false` to this block indicates the code is invalid and instructs the parking view to scan for more QR codes. This block may be called synchronously or asynchronously but must be done so on the main queue.

During a QR code scanning session, it is not possible to turn on the flashlight due to ARCore being used on the FMParkingView. ARCore blocks any input regarding turning on/off the flashlight during an AR session, limiting QR code readability on dark environments.

### Localizing

During localization, frames are continuously captured and sent to the server. Filtering logic in the SDK will automatically select the best frames, and it will issue behavior requests to the user to help improve the incoming images. Confidence in the location result increases during successive updates and clients can choose to stop localizing by dismissing the view, when a desired confidence level is reached.
```kotlin
override fun fmParkingView(result: FMLocationResult) {
    // Got a localization result
    // Localization will continue until you dismiss the view
    // You should decide on acceptable criteria for a result, 
    // one way is by checking the `confidence` value
    if (result.confidence == FMResultConfidence.LOW) {
        return
    }
    val coordinates = result.location.coordinate
    Log.d("Coordinates","$coordinates")
    
    // Medium or high confidence, dismiss to stop localizing
    fmParkingView.dismiss()
}
```

Localization errors may occur but the localization process will not stop and it is still possible to get a successful localization result. You should decide on an acceptable threshold for errors and only stop localizing when it is reached, again by dismissing the view.
```kotlin
override fun fmParkingView(error: ErrorResponse, metadata: Any?) {
    // Got a localization error
    errorCount += 1
    if (errorCount < 5) {
        return
    }
    // Too many errors, dismiss to stop localizing
    fmParkingView.dismiss()
}
```
### Customizing UI

The UI for both scanning QR codes and localizing can be completely customized by creating your own implementations of the view protocols.

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

### Behavior Requests

To help the user localize successfully and to maximize the result quality, camera input is filtered against common problems and behavior requests are displayed to the user. These are messages explaining what the user should be doing with their device in order to localize properly. For example, if the users device is aimed at the ground, you may receive a `"Tilt your device up"` request.

If you're using the default localization UI, these requests are already displayed to the user. If you've registered your own custom UI, you should use the `didRequestLocalizationBehavior(behavior: FMBehaviorRequest)` method of `FMLocalizingViewProtocol` to display these requests to users.

```kotlin
class MyCustomLocalizingView: FMLocalizingViewProtocol {

    var label: TextView

    override fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest) {
        // display the requested behavior to the user
        label.text = behavior.description
    }
}
```

As of right now behavior requests are only available in English. More languages coming soon.

### Testing and Debugging

Since it's not always possible to be onsite for testing, a simulation mode is provided to make test localization queries.
```kotlin
    fmParkingView.isSimulation = true
```
And for debugging, it's sometimes useful to show the statistics view to see what's happening under the hood.
```kotlin
    fmParkingView.showStatistics = true
```
    
### Overrides

For testing, the device location and server URL can be specified in the different flavours of the SDK

```kotlin
    buildConfigField "String", "FM_API_BASE_URL", "\"https://api.fantasmo.io/v1/image.localize\""
    
    buildConfigField "String", "FM_GPS_LAT_LONG", "\"48.848138681935886,2.371750713292894\""
```
