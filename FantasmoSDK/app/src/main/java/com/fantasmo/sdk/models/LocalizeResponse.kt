//
//  LocalizeResponse.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents a LocalizeResponse with the response data from the
 * localize request.
 * @property geofences
 * @property location
 * @property pose
 * @property uuid
 */
data class LocalizeResponse(
    val geofences: List<Geofence>?,
    val location: Location?,
    val pose: Pose?,
    val uuid: String?
)