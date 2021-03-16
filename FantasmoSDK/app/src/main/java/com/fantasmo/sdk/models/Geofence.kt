//
//  Geofence.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Data class for a Geofence
 * @property elementID
 * @property elementType
 */
data class Geofence(
    val elementID: Int,
    val elementType: String)