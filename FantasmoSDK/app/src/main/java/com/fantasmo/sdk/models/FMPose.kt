//
//  FMPose.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.models

import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI

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

         /**
         * Calculates pose of OpenCV anchor in the coordinate system of OpenCV camera.
         * @property anchorTransform Pose of anchor coordinate system in the world coordinate system.
         * @property cameraTransform Pose of camera coordinate system in the world coordinate system.
         */
        fun diffPose(anchorTransform: Pose, cameraTransform: Pose): FMPose {
            val transferToOpenCVAxis = FMUtility.makeRotation(PI.toFloat(), Vector3(0.0f, 0.0f, 1.0f))

            val openCVAnchorTransform = anchorTransform.compose(transferToOpenCVAxis)

            // Relative pose in world CS: R_w = A_w * C_w^(-1), then we transfer to CS of Camera.
            // Transform matrix is C_w^(-1), so R_c = C_w^(-1) * (A_w * C_w^(-1)) * C_w = C_w^(-1) * A_w
            val openCVAnchorTransformInCameraCS = cameraTransform.inverse().compose(openCVAnchorTransform)

            val openCVAnchorPoseInOpenCVCameraCS = FMPose(openCVAnchorTransformInCameraCS)
            return openCVAnchorPoseInOpenCVCameraCS
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
        val diffPosition = FMPosition.minus(this.position, toPose.position)
        val diffOrientation = this.orientation.difference(toPose.orientation)
        return FMPose(position = diffPosition, orientation = diffOrientation)
    }

    fun applyTransform(pose: FMPose) {
        this.position = FMPosition.minus(this.position, pose.position)
        this.orientation = this.orientation.rotate(pose.orientation)
    }
}