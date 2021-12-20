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
 * @property timestamp
 * @property horizontalAccuracy
 * @property verticalAccuracy
 * @property coordinate
 */
data class Location(
    var altitude: Any?,
    var timestamp: Any?,
    var horizontalAccuracy: Any?,
    var verticalAccuracy: Any?,
    var coordinate: Coordinate
) {
    constructor() : this(0, 0, 0, 0, Coordinate(0.0, 0.0))
}