//
//  Pose.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

/**
 * Represents a Pose.
 * @property accuracy
 * @property orientation
 * @property position
 */
internal data class Pose(
    val accuracy: String,
    val orientation: Orientation,
    val position: Position
)