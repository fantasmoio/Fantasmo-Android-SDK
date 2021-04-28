package com.fantasmo.sdk.frameSequenceFilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.MovingAverage
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Class responsible for filtering frames due to blur on images.
 * Prevents from sending blurred images.
 */
class FMBlurFilterRule(private val context: Context) : FMFrameSequenceFilterRule {

    private val TAG = "FMBlurFilter"

    private val CLASSIC_MATRIX = floatArrayOf(
        -1.0f, -1.0f, -1.0f,
        -1.0f,  8.0f, -1.0f,
        -1.0f, -1.0f, -1.0f
    )

    var variance: Double = 0.0
    var varianceAverager = MovingAverage()
    var averageVariance = varianceAverager.average

    var varianceThreshold = 45.0
    var suddenDropThreshold = 60.0

    var throughputAverager = MovingAverage(8)
    var averageThroughput: Double = throughputAverager.average
    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooFast failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        variance = calculateVariance(arFrame)
        varianceAverager.addSample(variance)

        val isLowVariance: Boolean

        val isBelowThreshold = variance > varianceThreshold
        val isSuddenDrop = variance > (averageVariance - suddenDropThreshold)
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
            val rs = RenderScript.create(context)

            // Run edge detection algorithm using a laplacian matrix convolution
            // Apply 3x3 convolution to detect edges
            val edgesBitmap = Bitmap.createBitmap(imageBitmap.width,
                imageBitmap.height,
                imageBitmap.config
            )
            val sourceAllocation = Allocation.createFromBitmap(rs,
                imageBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val edgesTargetAllocation = Allocation.createFromBitmap(rs,
                edgesBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )

            val convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
            convolve.setInput(sourceAllocation)
            convolve.setCoefficients(CLASSIC_MATRIX)
            convolve.forEach(edgesTargetAllocation)
            edgesTargetAllocation.copyTo(edgesBitmap)

            edgesBitmap.setHasAlpha(false)
            val pixels = IntArray(edgesBitmap.height * edgesBitmap.width)
            edgesBitmap.getPixels(pixels, 0, edgesBitmap.width, 0, 0, edgesBitmap.width, edgesBitmap.height)

            val stdDev = countBlackPixels(edgesBitmap)

            // Get variance from std which is the result of last operation
            Log.d(TAG,"Variance result -> $stdDev")
            return stdDev

        }catch(e:NotYetAvailableException){
            Log.e(TAG,"FrameNotAvailable")
        }
        return 0.0
    }

    private fun countBlackPixels(bitmap: Bitmap): Double{
        val pixels = IntArray(bitmap.height * bitmap.width)
        Log.d("AMOUNT OF PIXELS:", "${pixels.size}")
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var count = 0

        for(pixel in pixels){
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            if(r == 0 && g == 0 && b == 0){
                count++
            }
        }
        Log.d("AMOUNT OF BLACK PIXELS:", "$count")
        val percent = (count.toDouble()/(pixels.size)) * 100
        Log.d("PERCENTAGE", "$percent")
        return percent
    }
}