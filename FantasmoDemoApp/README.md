# Fantasmo-Android
Demo application to demonstrate how to interact with and test the Fantasmo Android SDK.


## AAR Library
The SDK library is located inside the libs folder. When changing the SDK, the .aar should be replaced with the new version. 

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
