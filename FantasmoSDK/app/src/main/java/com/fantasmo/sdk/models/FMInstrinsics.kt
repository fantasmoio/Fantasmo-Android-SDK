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
 */
internal class FMIntrinsics() {

    var fx: Float = 0.0f
    var fy: Float = 0.0f
    var cx: Float = 0.0f
    var cy: Float = 0.0f

    constructor(fx: Float, fy: Float, cx: Float, cy: Float) : this() {
        this.fx = fx
        this.fy = fy
        this.cx = cx
        this.cy = cy
    }

    constructor(
        intrinsics: Array<Array<Float>>,
        atScale: Float,
        interfaceOrientation: Int,
        deviceOrientation: Int,
        frameWidth: Int,
        frameHeight: Int
    ) : this() {
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
                cx = intrinsics[2][1]
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
}
