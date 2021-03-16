//
//  Location.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents a Location.
 * @property altitude
 * @property coordinate
 * @property floor
 * @property heading
 * @property horizontalAccuracy
 * @property verticalAccuracy
 */
data class Location(
    val altitude: Any,
    val coordinate: Coordinate,
    val floor: Any,
    val heading: Any,
    val horizontalAccuracy: Any,
    val verticalAccuracy: Any
)