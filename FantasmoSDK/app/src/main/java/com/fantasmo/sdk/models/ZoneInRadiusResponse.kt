//
//  ZoneInRadiusResponse.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents a ZoneInRadiusResponse with the response data from the
 * zone in radius request check.
 * @property result the result of the check with "true" if the zone is in the radius and
 * false otherwise.
 */
data class ZoneInRadiusResponse(
    val result: String?
)