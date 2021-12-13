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
    var altitude: Any?,
    var coordinate: Coordinate,
    var floor: Any?,
    var heading: Any?,
    var horizontalAccuracy: Any?,
    var verticalAccuracy: Any?
) {
    constructor() : this(0, Coordinate(0.0, 0.0), 0, 0, 0, 0)
}