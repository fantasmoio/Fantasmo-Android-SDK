//
//  FMZone.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents semantic zones corresponding to a position.
 * @property zoneType
 * @property id
 */
data class FMZone(private val _zoneType: ZoneType, val id: String?)  {

    var zoneType: String = _zoneType.name

    enum class ZoneType {
        STREET,
        SIDEWALK,
        FURNITURE,
        CROSSWALK,
        ACESSRAMP,
        AUTOPARKING,
        BUSSTOP,
        PLANTER,
        PARKING,
        UNKOWN;
    }
}
