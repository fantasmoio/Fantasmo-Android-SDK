//
//  FMPose.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Quaternion

/**
 * Device pose at the moment of image capture. The coordinate frame is
 * right-handed, Y down (i.e., OpenCV convention). Y is gravity aligned. Rotation
 * around Y axis is relative to true north.
 */
class FMPose {

    var source: String = "device"
    var position: FMPosition
    var orientation: FMOrientation
    var confidence: String = ""

    companion object {
        // Extracts the position and orientation from an ARKit camera transform matrix and converts

        // from ARCore coordinates (right-handed, Y Up) to OpenCV coordinates (right-handed, Y Down)
        fun interpolatePoses(
            startingPose: FMPose,
            endingPose: FMPose,
            allPoses: List<FMPose>
        ): List<FMPose> {
            if ((allPoses.size <= 1)) {
                return listOf(endingPose)
            }
            // Get the starting difference
            val S =
                startingPose.diffPose(toPose = allPoses[0]) //Note: This should be 0, because the startingPose
            // is by definition the first pose in the allPoses vector

            val E = endingPose.diffPose(toPose = allPoses.lastOrNull()!!)
            val D = E.diffPose(toPose = S)
            var cumulativeSum = 0.0f
            val distances = mutableListOf<Float>()
            distances.add(0.0f)

            for (poseIndex in 1 until (allPoses.size)) {
                val thisDistance =
                    allPoses[poseIndex - 1].position.distance(allPoses[poseIndex].position)
                cumulativeSum += thisDistance.toFloat()
                distances.add(cumulativeSum)
            }

            if (cumulativeSum == 0f) {
                cumulativeSum = 1f
            }

            val interpolatedPoses = mutableListOf<FMPose>()
            for (poseIndex in 0 until (allPoses.size)) {
                val thisDistance = distances[poseIndex] / cumulativeSum
                val newPose = allPoses[poseIndex].interpolated(
                    distance = thisDistance,
                    startPose = S,
                    differencePose = D
                )
                interpolatedPoses.add(newPose)
            }

            return interpolatedPoses
        }

        fun diffPose(anchorTransform: Pose, cameraTransform: Pose): FMPose {
            val anchorPose = FMPose(anchorTransform)
            val cameraPose = FMPose(cameraTransform)

            val anchorPoseQuaternion = anchorPose.orientation.toQuaternion().normalized()
            val cameraPoseQuaternion = cameraPose.orientation.toQuaternion().normalized()

            val resultPoseQuaternion = Quaternion.multiply(cameraPoseQuaternion.inverted(), anchorPoseQuaternion)
            val resultPoseOrientation = FMOrientation(resultPoseQuaternion.w, resultPoseQuaternion.x, resultPoseQuaternion.y, resultPoseQuaternion.z)
            val resultPosePosition = FMPosition(resultPoseQuaternion.x, resultPoseQuaternion.y, resultPoseQuaternion.z)

            return FMPose(resultPosePosition, resultPoseOrientation, "")
        }
    }

    constructor(
        position: FMPosition,
        orientation: FMOrientation,
        confidence: String = ""
    ) : this() {
        this.position = position
        this.orientation = orientation
        this.confidence = confidence
    }

    constructor() {
        this.position = FMPosition(0.0, 0.0, 0.0)
        this.orientation = FMOrientation(0.0f, 0.0f, 0.0f, 0.0f)
    }

    constructor(pose: Pose) {
        this.position = FMPosition(pose.translation)
        this.orientation = FMOrientation(pose.rotationQuaternion)
    }

    override fun toString(): String =
        "Position [${position.toString()}]   Orientation [${orientation.toString()}]   Confidence [${confidence}]"

    private fun interpolated(distance: Float, startPose: FMPose, differencePose: FMPose): FMPose {
        val resultPosition = this.position.interpolated(
            distance = distance,
            startPosition = startPose.position,
            differencePosition = differencePose.position
        )
        val resultOrientation = this.orientation.interpolated(
            distance = distance,
            startOrientation = startPose.orientation,
            differenceOrientation = differencePose.orientation
        )
        return FMPose(position = resultPosition, orientation = resultOrientation)
    }

    fun diffPose(toPose: FMPose): FMPose {
        val diffPosition = this.position.minus(this.position, toPose.position)
        val diffOrientation = this.orientation.difference(toPose.orientation)
        return FMPose(position = diffPosition, orientation = diffOrientation)
    }

    fun applyTransform(pose: FMPose) {
        this.position = this.position.minus(this.position, pose.position)
        this.orientation = this.orientation.rotate(pose.orientation)
    }
}