//
//  FMPosition.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Position of the device at the moment of image capture . Units are meters.
 */
internal class FMPosition {

    var x: Float = 0.0f
    var y: Float = 0.0f
    var z: Float = 0.0f

    constructor(x: Double, y: Double, z: Double) {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.z = z.toFloat()
    }

    constructor(pos: FMPosition) {
        this.x = pos.x
        this.y = pos.y
        this.z = pos.z
    }

    /**
     * Extracts the position from an ARCore camera transform matrix and converts
     * from android coordinates (right-handed, Y Up) to OpenCV coordinates (right-handed, Y Down)
     */
    constructor(translation: FloatArray) {
        x = translation[0]
        y = -translation[1]
        z = -translation[2]
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    companion object {
        fun plus(left: FMPosition, right: FMPosition): FMPosition {
            val sx = left.x + right.x
            val sy = left.y + right.y
            val sz = left.z + right.z
            return FMPosition(sx, sy, sz)
        }

        fun minus(left: FMPosition, right: FMPosition): FMPosition {
            val sx = left.x - right.x
            val sy = left.y - right.y
            val sz = left.z - right.z
            return FMPosition(sx, sy, sz)
        }

        fun plusEquals(left: FMPosition, right: FMPosition) {
            left.x += right.x
            left.y += right.y
            left.z += right.z
        }

        fun divide(left: FMPosition, right: Double): FMPosition {
            val sx = left.x / right
            val sy = left.y / right
            val sz = left.z / right
            return FMPosition(sx, sy, sz)
        }

        fun divide(left: FMPosition, right: Float): FMPosition {
            val sx = left.x / right
            val sy = left.y / right
            val sz = left.z / right
            return FMPosition(sx, sy, sz)
        }

        fun divide(left: FMPosition, right: Int): FMPosition {
            val sx = left.x / right
            val sy = left.y / right
            val sz = left.z / right
            return FMPosition(sx, sy, sz)
        }

        fun multiply(left: FMPosition, right: Double): FMPosition {
            val sx = left.x * right
            val sy = left.y * right
            val sz = left.z * right
            return FMPosition(sx, sy, sz)
        }

        fun multiply(left: FMPosition, right: Float): FMPosition {
            val sx = left.x * right
            val sy = left.y * right
            val sz = left.z * right
            return FMPosition(sx, sy, sz)
        }

        fun multiply(left: FMPosition, right: Int): FMPosition {
            val sx = left.x * right
            val sy = left.y * right
            val sz = left.z * right
            return FMPosition(sx, sy, sz)
        }
    }

    fun interpolated(
        distance: Float,
        startPosition: FMPosition,
        differencePosition: FMPosition
    ): FMPosition {
        val resultX = this.x + startPosition.x + distance * differencePosition.x
        val resultY = this.y + startPosition.y + distance * differencePosition.y
        val resultZ = this.z + startPosition.z + distance * differencePosition.z
        return FMPosition(resultX, resultY, resultZ)
    }

    override fun toString(): String = "x: $x :: y: $y :: z: $z"

    fun distance(toPosition: FMPosition): Double =
        sqrt(
            (this.x - toPosition.x).pow(2.0f) +
                    (this.y - toPosition.y).toDouble().pow(2.0)
                    + (this.z - toPosition.z).toDouble().pow(2.0)
        )

    fun norm(): Float = sqrt(x * x + y * y + z * z)
}
