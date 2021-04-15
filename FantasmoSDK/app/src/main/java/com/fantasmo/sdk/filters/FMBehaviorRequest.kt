package com.fantasmo.sdk.filters

enum class FMBehaviorRequest(val displayName : String) {
    TILTUP("Tilt your device up"),
    TILTDOWN("Tilt your device down"),
    PANAROUND("Pan around the scene"),
    PANSLOWLY("Pan more slowly");
}
