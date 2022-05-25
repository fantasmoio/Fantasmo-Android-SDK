//
//  FMOrientation.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

import android.util.Log
import com.fantasmo.sdk.utilities.math.Quaternion
import com.fantasmo.sdk.utilities.math.Vector3
import kotlin.math.PI

/**
 * Orientation of the device at moment of image capture.
 */
internal class FMOrientation {

    var x: Float
    var y: Float
    var z: Float
    var w: Float

    companion object {
        val TAG = "FMOrientation"

        fun getAverageQuaternion(quaternions: List<FMOrientation>): FMOrientation? {
            if (quaternions.isNullOrEmpty()) {
                return null
            } else {
                val firstQuaternion = quaternions[0]
                var numberOfQuaternionsSummedUp = 1.0f
                for (i in 1 until quaternions.size) {
                    val quaternion = quaternions[i]
                    if ((quaternion.quaternionDot(firstQuaternion) < 0)) {
                        quaternion.flipSign()
                    }
                    if ((firstQuaternion.angularDistance(quaternion) < 10)) {
                        Log.d(TAG, "Valid quaternion for averaging")
                        numberOfQuaternionsSummedUp += 1.0f
                        firstQuaternion.w += quaternion.w
                        firstQuaternion.x += quaternion.x
                        firstQuaternion.y += quaternion.y
                        firstQuaternion.z += quaternion.z
                    } else {
                        Log.w(TAG, "Invalid quaternion for averaging")
                    }
                }
                firstQuaternion.w /= numberOfQuaternionsSummedUp
                firstQuaternion.x /= numberOfQuaternionsSummedUp
                firstQuaternion.y /= numberOfQuaternionsSummedUp
                firstQuaternion.z /= numberOfQuaternionsSummedUp
                firstQuaternion.normalize()
                return firstQuaternion
            }
        }
    }

    /**
     * Extracts the orientation from an ARCore camera transform matrix and converts
     * from ARCore coordinates (right-handed, Y Up) to OpenCV coordinates (right-handed, Y Down)
     */
    constructor(w: Float, x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(rotation: Quaternion) {
        this.x = rotation.x
        this.y = rotation.y
        this.z = rotation.z
        this.w = rotation.w
    }

    constructor(rotationQuaternion: FloatArray) {
        this.x = rotationQuaternion[0]
        this.y = -rotationQuaternion[1]
        this.z = -rotationQuaternion[2]
        this.w = rotationQuaternion[3]
    }

    fun toQuaternion(): Quaternion = Quaternion(this.x, this.y, this.z, this.w)

    private fun getRotationTo(orientation: FMOrientation): Quaternion =
        Quaternion.multiply(this.toQuaternion(), orientation.toQuaternion())


    fun difference(orientation: FMOrientation): FMOrientation {
        val thisDifference =
            Quaternion.multiply(this.toQuaternion(), orientation.toQuaternion().inverted())
        return FMOrientation(
            thisDifference.w, thisDifference.x, thisDifference.y, thisDifference.w
        )
    }

    private fun hamiltonProduct(quaternionRotation: Quaternion): FMOrientation {
        val a1 = quaternionRotation.w
        val b1 = quaternionRotation.x
        val c1 = quaternionRotation.y
        val d1 = quaternionRotation.z
        val a2 = this.w
        val b2 = this.x
        val c2 = this.y
        val d2 = this.z
        val hamiltonW = a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2
        val hamiltonX = a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2
        val hamiltonY = a1 * c2 - b1 * d2 + c1 * a2 + d1 * b2
        val hamiltonZ = a1 * d2 + b1 * c2 - c1 * b2 + d1 * a2
        return FMOrientation(hamiltonW, hamiltonX, hamiltonY, hamiltonZ)
    }

    private fun hamiltonProduct(quaternionRotation: Array<Double>): FMOrientation {
        val a1 = quaternionRotation[3].toFloat()
        val b1 = quaternionRotation[0].toFloat()
        val c1 = quaternionRotation[1].toFloat()
        val d1 = quaternionRotation[2].toFloat()
        val a2 = this.w
        val b2 = this.x
        val c2 = this.y
        val d2 = this.z
        val hamiltonW = a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2
        val hamiltonX = a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2
        val hamiltonY = a1 * c2 - b1 * d2 + c1 * a2 + d1 * b2
        val hamiltonZ = a1 * d2 + b1 * c2 - c1 * b2 + d1 * a2
        return FMOrientation(hamiltonW, hamiltonX, hamiltonY, hamiltonZ)
    }

    private fun hamiltonProduct(quaternionRotation: FMOrientation): FMOrientation {
        val a1 = quaternionRotation.w
        val b1 = quaternionRotation.x
        val c1 = quaternionRotation.y
        val d1 = quaternionRotation.z
        val a2 = this.w
        val b2 = this.x
        val c2 = this.y
        val d2 = this.z
        val hamiltonW = a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2
        val hamiltonX = a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2
        val hamiltonY = a1 * c2 - b1 * d2 + c1 * a2 + d1 * b2
        val hamiltonZ = a1 * d2 + b1 * c2 - c1 * b2 + d1 * a2
        return FMOrientation(hamiltonW, hamiltonX, hamiltonY, hamiltonZ)
    }

    fun interpolated(
        distance: Float,
        startOrientation: FMOrientation,
        differenceOrientation: FMOrientation
    ): FMOrientation {
        val differenceQuaternion = differenceOrientation.toQuaternion()
        val iq = Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
        val resultOrientation = Quaternion.multiply(
            Quaternion.multiply(iq, startOrientation.toQuaternion()),
            this.toQuaternion()
        )
        return FMOrientation(resultOrientation)
    }

    override fun toString(): String = "x: $x :: y:$y :: z: $z :: w: $w"

    fun rotate(rot: FMOrientation): FMOrientation =
        hamiltonProduct(rot)

    fun rotate(pos: FMPosition): FMPosition {
        val q = this.toQuaternion()
        val p = Vector3(pos.x, pos.y, pos.z)
        val pRot = Quaternion.rotateVector(q, p)
        return FMPosition(pRot.x, pRot.y, pRot.z)
    }

    fun inverse(): FMOrientation {
        val q = this.toQuaternion()
        return FMOrientation(q.inverted())
    }

    fun angularDistance(other: FMOrientation): Double {
        val qd = getRotationTo(other)
        return qd.w * PI / 180.0
    }

    fun flipSign() {
        this.w = -this.w
        this.x = -this.x
        this.y = -this.y
        this.z = -this.z
    }

    fun normalize() {
        val lengthD: Float = 1.0f / (w * w + x * x + y * y + z * z)
        w *= lengthD
        x *= lengthD
        y *= lengthD
        z *= lengthD
    }

    fun quaternionDot(other: FMOrientation): Float =
        (w * other.w + x * other.x + y * other.y + z * other.z)
}