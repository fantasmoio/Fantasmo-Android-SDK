//
//  FMIntrinsics.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

import android.view.Surface

/**
 * Represents camera intrinsics. The intrinsics change on a frame-to-frame basis which requires they
 * are parsed for each frame.
 * @property intrinsics
 * @property atScale
 * @property interfaceOrientation
 * @property deviceOrientation
 * @property frameWidth
 * @property frameHeight
 */
data class FMIntrinsics(
    private val intrinsics: Array<Array<Float>>,
    private val atScale: Float,
    private val interfaceOrientation: Int,
    private val deviceOrientation: Int,
    private val frameWidth: Int,
    private val frameHeight: Int
) {

    var fx: Float = 0.0f
    var fy: Float = 0.0f
    var cx: Float = 0.0f
    var cy: Float = 0.0f

    init {
        when (deviceOrientation) {
            // SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            Surface.ROTATION_270 -> {
                fx = intrinsics[0][0]
                fy = intrinsics[1][1]
                cx = intrinsics[2][0]
                cy = intrinsics[2][1]
            }
            // SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_90 -> {
                fx = intrinsics[0][0]
                fy = intrinsics[1][1]
                cx = frameWidth - intrinsics[2][0]
                cy = frameHeight - intrinsics[2][1]
            }
            // SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_0 -> {
                fx = intrinsics[1][1]
                fy = intrinsics[0][0]
                cx = intrinsics[2][2]
                cy = intrinsics[2][0]
            }
            // SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_180 -> {
                fx = intrinsics[1][1]
                fy = intrinsics[0][0]
                cx = frameHeight - intrinsics[2][1]
                cy = frameWidth - intrinsics[2][0]
            }
            else -> {
                fx = intrinsics[1][1]
                fy = intrinsics[0][0]
                cx = intrinsics[2][1]
                cy = intrinsics[2][0]
            }
        }

        fx *= atScale
        fy *= atScale
        cx *= atScale
        cy *= atScale
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FMIntrinsics

        if (!intrinsics.contentDeepEquals(other.intrinsics)) return false
        if (atScale != other.atScale) return false
        if (interfaceOrientation != other.interfaceOrientation) return false
        if (deviceOrientation != other.deviceOrientation) return false
        if (frameWidth != other.frameWidth) return false
        if (frameHeight != other.frameHeight) return false
        if (fx != other.fx) return false
        if (fy != other.fy) return false
        if (cx != other.cx) return false
        if (cy != other.cy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intrinsics.contentDeepHashCode()
        result = 31 * result + atScale.hashCode()
        result = 31 * result + interfaceOrientation
        result = 31 * result + deviceOrientation
        result = 31 * result + frameWidth
        result = 31 * result + frameHeight
        result = 31 * result + fx.hashCode()
        result = 31 * result + fy.hashCode()
        result = 31 * result + cx.hashCode()
        result = 31 * result + cy.hashCode()
        return result
    }
}
