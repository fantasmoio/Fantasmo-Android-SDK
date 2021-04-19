package com.fantasmo.sdk.frameSequenceFilter

import android.graphics.BitmapFactory
import android.util.Log
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.MovingAverage
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import java.text.DecimalFormat
import kotlin.math.pow

class FMBlurFilterRule : FMFrameSequenceFilterRule {
    private val TAG = "FMBlurFilterRule"

    var variance: Double = 0.0
    var varianceAverager = MovingAverage()
    var averageVariance = varianceAverager.average

    var varianceThreshold = 200.0
    var suddenDropThreshold = 100.0

    var throughputAverager = MovingAverage(8)
    var averageThroughput: Double = throughputAverager.average

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        variance = calculateVariance(arFrame)
        varianceAverager.addSample(variance)

        val isLowVariance: Boolean

        val isBelowThreshold = variance < varianceThreshold
        val isSuddenDrop = variance < (averageVariance - suddenDropThreshold)
        isLowVariance = isBelowThreshold || isSuddenDrop

        if (isLowVariance) {
            throughputAverager.addSample(0.0)
        } else {
            throughputAverager.addSample(1.0)
        }

        // if not enough images are passing, pass regardless of variance
        val isBlurry: Boolean = if (averageThroughput < 0.25) {
            false
        } else {
            isLowVariance
        }

        return if (isBlurry) {
            Pair(FMFrameFilterResult.REJECTED,FMFrameFilterFailure.MOVINGTOOFAST)
        } else {
            Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
        }
    }

    private fun calculateVariance(arFrame: Frame): Double {
        try{

            val cameraImage = arFrame.acquireCameraImage()

            val baOutputStream = FMUtility.createByteArrayOutputStream(cameraImage)

            // Release the image
            cameraImage.close()

            val imageBytes: ByteArray = baOutputStream.toByteArray()
            val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val destination = Mat()
            val matGray = Mat()
            val sourceMatImage = Mat()

            // Convert bitmap to Matrix
            Utils.bitmapToMat(imageBitmap, sourceMatImage)
            // Convert sourceImage to grayscale
            Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY)
            // Focus measure of the image using Variance of Laplacian
            Imgproc.Laplacian(matGray, destination, 3)
            val median = MatOfDouble()
            val std = MatOfDouble()
            // Calculate mean and standard deviation of array elements
            Core.meanStdDev(destination, median, std)

            // Get variance from std which is the result of last operation
            val result = DecimalFormat("0.00").format(std.get(0, 0)[0].pow(2.0)).toDouble()
            Log.d(TAG,"Variance result -> $result")
            return result

        }catch(e:NotYetAvailableException){
            Log.e(TAG,"FrameNotAvailable")
        }
        return 0.0
    }

}