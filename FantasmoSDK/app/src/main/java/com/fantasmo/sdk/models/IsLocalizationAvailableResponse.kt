//
//  IsLocalizationAvailableResponse.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents a ZoneInRadiusResponse with the response data from the
 * zone in radius request check.
 * @property available the result of the check with "true" if the zone is in the radius and
 * false otherwise.
 */
internal data class IsLocalizationAvailableResponse(
    val available: String?
)