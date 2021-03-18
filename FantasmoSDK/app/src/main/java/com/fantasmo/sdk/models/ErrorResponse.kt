//
//  ErrorResponse.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents an error response.
 * @property code
 * @property message
 */
data class ErrorResponse(
    val code: Int,
    val message: String?
)