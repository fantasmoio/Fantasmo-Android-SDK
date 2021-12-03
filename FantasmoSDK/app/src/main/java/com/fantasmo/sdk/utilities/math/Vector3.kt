package com.fantasmo.sdk.utilities.math

import com.fantasmo.sdk.utilities.math.MathHelper.almostEqualRelativeAndAbs
import com.fantasmo.sdk.utilities.math.MathHelper.clamp
import com.fantasmo.sdk.utilities.math.MathHelper.lerp
import kotlin.math.acos
import kotlin.math.sqrt

class Vector3 {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

    /** Construct a Vector3 and assign zero to all values  */
    constructor () {
        x = 0f
        y = 0f
        z = 0f
    }

    /** Construct a Vector3 and assign each value  */
    constructor (x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    /** Construct a Vector3 and copy the values  */
    constructor (v: Vector3?) {
        Preconditions.checkNotNull(v, "Parameter \"v\" was null.")
        if (v != null) {
            set(v)
        }
    }

    /** Copy the values from another Vector3 to this Vector3  */
    fun set(v: Vector3) {
        Preconditions.checkNotNull(v, "Parameter \"v\" was null.")
        x = v.x
        y = v.y
        z = v.z
    }

    /** Set each value  */
    operator fun set(vx: Float, vy: Float, vz: Float) {
        x = vx
        y = vy
        z = vz
    }

    /** Set each value to zero  */
    fun setZero() {
        set(0f, 0f, 0f)
    }

    /** Set each value to one  */
    fun setOne() {
        set(1f, 1f, 1f)
    }

    /** Forward into the screen is the negative Z direction  */
    fun setForward() {
        set(0f, 0f, -1f)
    }

    /** Back out of the screen is the positive Z direction  */
    fun setBack() {
        set(0f, 0f, 1f)
    }

    /** Up is the positive Y direction  */
    fun setUp() {
        set(0f, 1f, 0f)
    }

    /** Down is the negative Y direction  */
    fun setDown() {
        set(0f, -1f, 0f)
    }

    /** Right is the positive X direction  */
    fun setRight() {
        set(1f, 0f, 0f)
    }

    /** Left is the negative X direction  */
    fun setLeft() {
        set(-1f, 0f, 0f)
    }

    fun lengthSquared(): Float {
        return x * x + y * y + z * z
    }

    fun length(): Float {
        return sqrt(lengthSquared().toDouble()).toFloat()
    }

    override fun toString(): String {
        return "[x=$x, y=$y, z=$z]"
    }

    /** Scales the Vector3 to the unit length  */
    fun normalized(): Vector3 {
        val result = this
        val normSquared: Float = dot(this, this)
        if (almostEqualRelativeAndAbs(normSquared, 0.0f)) {
            result.setZero()
        } else if (normSquared != 1f) {
            val norm = (1.0 / sqrt(normSquared.toDouble())).toFloat()
            result.set(this.scaled(norm))
        }
        return result
    }

    /**
     * Uniformly scales a Vector3
     *
     * @return a Vector3 multiplied by a scalar amount
     */
    fun scaled(a: Float): Vector3 {
        return Vector3(x * a, y * a, z * a)
    }

    /**
     * Negates a Vector3
     *
     * @return A Vector3 with opposite direction
     */
    fun negated(): Vector3 {
        return Vector3(-x, -y, -z)
    }

    /**
     * Adds two Vector3's
     *
     * @return The combined Vector3
     */
    fun add(lhs: Vector3, rhs: Vector3): Vector3 {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        return Vector3(lhs.x + rhs.x, lhs.y + rhs.y, lhs.z + rhs.z)
    }

    /**
     * Subtract two Vector3
     *
     * @return The combined Vector3
     */
    fun subtract(lhs: Vector3, rhs: Vector3): Vector3 {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        return Vector3(lhs.x - rhs.x, lhs.y - rhs.y, lhs.z - rhs.z)
    }



    /** Get a Vector3 with each value set to the element wise maximum of two Vector3's values  */
    fun max(lhs: Vector3, rhs: Vector3): Vector3 {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        return Vector3(
            kotlin.math.max(lhs.x, rhs.x),
            kotlin.math.max(lhs.y, rhs.y),
            kotlin.math.max(lhs.z, rhs.z)
        )
    }

    /** Get the maximum value in a single Vector3  */
    fun componentMax(a: Vector3): Float {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.")
        return kotlin.math.max(kotlin.math.max(a.x, a.y), a.z)
    }

    /** Get the minimum value in a single Vector3  */
    fun componentMin(a: Vector3): Float {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.")
        return kotlin.math.min(kotlin.math.min(a.x, a.y), a.z)
    }

    /**
     * Linearly interpolates between a and b.
     *
     * @param a the beginning value
     * @param b the ending value
     * @param t ratio between the two floats.
     * @return interpolated value between the two floats
     */
    fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.")
        Preconditions.checkNotNull(b, "Parameter \"b\" was null.")
        return Vector3(
            lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t)
        )
    }

    /**
     * Returns the shortest angle in degrees between two vectors. The result is never greater than 180
     * degrees.
     */
    fun angleBetweenVectors(a: Vector3, b: Vector3): Float {
        val lengthA = a.length()
        val lengthB = b.length()
        val combinedLength = lengthA * lengthB
        if (almostEqualRelativeAndAbs(combinedLength, 0.0f)) {
            return 0.0f
        }
        val dot: Float = dot(a, b)
        var cos = dot / combinedLength

        // Clamp due to floating point precision that could cause dot to be > combinedLength.
        // Which would cause acos to return NaN.
        cos = clamp(cos, -1.0f, 1.0f)
        val angleRadians = acos(cos.toDouble()).toFloat()
        return Math.toDegrees(angleRadians.toDouble()).toFloat()
    }

    /** Compares two Vector3's are equal if each component is equal within a tolerance.  */
    fun equals(lhs: Vector3, rhs: Vector3): Boolean {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        var result = true
        result = result and almostEqualRelativeAndAbs(lhs.x, rhs.x)
        result = result and almostEqualRelativeAndAbs(lhs.y, rhs.y)
        result = result and almostEqualRelativeAndAbs(lhs.z, rhs.z)
        return result
    }

    /** @hide
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + java.lang.Float.floatToIntBits(x)
        result = prime * result + java.lang.Float.floatToIntBits(y)
        result = prime * result + java.lang.Float.floatToIntBits(z)
        return result
    }

companion object{
    /**
     * Get dot product of two Vector3's
     *
     * @return The scalar product of the Vector3's
     */
    fun dot(lhs: Vector3, rhs: Vector3): Float {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        return lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z
    }

    /**
     * Get cross product of two Vector3's
     *
     * @return A Vector3 perpendicular to Vector3's
     */
    fun cross(lhs: Vector3, rhs: Vector3): Vector3 {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        val lhsX = lhs.x
        val lhsY = lhs.y
        val lhsZ = lhs.z
        val rhsX = rhs.x
        val rhsY = rhs.y
        val rhsZ = rhs.z
        return Vector3(
            lhsY * rhsZ - lhsZ * rhsY, lhsZ * rhsX - lhsX * rhsZ, lhsX * rhsY - lhsY * rhsX
        )
    }

    /** Get a Vector3 with each value set to the element wise minimum of two Vector3's values  */
    fun min(lhs: Vector3, rhs: Vector3): Vector3 {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.")
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.")
        return Vector3(
            kotlin.math.min(lhs.x, rhs.x),
            kotlin.math.min(lhs.y, rhs.y),
            kotlin.math.min(lhs.z, rhs.z)
        )
    }
    /** Gets a Vector3 with all values set to zero  */
    fun zero(): Vector3 {
        return Vector3()
    }

    /** Gets a Vector3 with all values set to one  */
    fun one(): Vector3 {
        val result = Vector3()
        result.setOne()
        return result
    }

    /** Gets a Vector3 set to (0, 0, -1)  */
    fun forward(): Vector3 {
        val result = Vector3()
        result.setForward()
        return result
    }

    /** Gets a Vector3 set to (0, 0, 1)  */
    fun back(): Vector3 {
        val result = Vector3()
        result.setBack()
        return result
    }

    /** Gets a Vector3 set to (0, 1, 0)  */
    fun up(): Vector3 {
        val result = Vector3()
        result.setUp()
        return result
    }

    /** Gets a Vector3 set to (0, -1, 0)  */
    fun down(): Vector3 {
        val result = Vector3()
        result.setDown()
        return result
    }

    /** Gets a Vector3 set to (1, 0, 0)  */
    fun right(): Vector3 {
        val result = Vector3()
        result.setRight()
        return result
    }

    /** Gets a Vector3 set to (-1, 0, 0)  */
    fun left(): Vector3 {
        val result = Vector3()
        result.setLeft()
        return result
    }
}


    /**
     * Returns true if the other object is a Vector3 and each component is equal within a tolerance.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vector3

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }
}

