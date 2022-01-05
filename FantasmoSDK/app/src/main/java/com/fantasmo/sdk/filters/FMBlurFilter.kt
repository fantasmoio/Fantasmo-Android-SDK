package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.MovingAverage
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class responsible for filtering frames due to blur on images.
 * Prevents from sending blurred images.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMBlurFilter(
    context: Context
) : FMFrameFilter {

    private val laplacianMatrix = floatArrayOf(
        0.0f, 1.0f, 0.0f,
        1.0f, -4.0f, 1.0f,
        0.0f, 1.0f, 0.0f
    )

    private var variance: Float = 0.0f
    private var varianceAverager = MovingAverage()
    private var averageVariance = varianceAverager.average

    private var varianceThreshold = 275.0
    private var suddenDropThreshold = 0.4
    private var averageThroughputThreshold = 0.25

    private var throughputAverager = MovingAverage(8)
    private var averageThroughput: Float = throughputAverager.average

    private val rs = RenderScript.create(context)
    private val colorIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
    private val convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with ImageToBlurry failure
     */
    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        val byteArrayFrame = FMUtility.acquireFrameImage(arFrame)
        GlobalScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            variance = calculateVariance(byteArrayFrame)
        }
        varianceAverager.addSample(variance)

        val isLowVariance: Boolean

        val isBelowThreshold = variance < varianceThreshold
        val isSuddenDrop = variance < (averageVariance - suddenDropThreshold)
        isLowVariance = isBelowThreshold || isSuddenDrop

        if (isLowVariance) {
            throughputAverager.addSample(0.0f)
        } else {
            throughputAverager.addSample(1.0f)
        }

        // if not enough images are passing, pass regardless of variance
        val isBlurry: Boolean = if (averageThroughput < averageThroughputThreshold) {
            false
        } else {
            isLowVariance
        }

        return if (isBlurry) {
            FMUtility.setFrame(null)
            FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGETOOBLURRY)
        } else {
            FMUtility.setFrame(byteArrayFrame)
            FMFrameFilterResult.Accepted
        }
    }

    /**
     * Calculates the variance using image convolution
     * Takes the frame and acquire the image from it and turns into greyscale
     * After that applies edge detection matrix to the greyscale image and
     * calculate variance from that
     * @param byteArrayFrame frame converted to ByteArray to measure the variance
     * @return variance blurriness value
     * */
    private suspend fun calculateVariance(byteArrayFrame: ByteArray?): Float {
        val reducedHeight = 480
        val reducedWidth = 640
        if (byteArrayFrame == null) {
            return 0.0f
        } else {
            val stdDev = GlobalScope.async {

                val originalBitmap =
                    BitmapFactory.decodeByteArray(byteArrayFrame, 0, byteArrayFrame.size)
                val reducedBitmap =
                    Bitmap.createScaledBitmap(originalBitmap, reducedWidth, reducedHeight, true)

                // Greyscale so we're only dealing with white <--> black pixels,
                // this is so we only need to detect pixel luminosity
                val greyscaleBitmap = Bitmap.createBitmap(
                    reducedBitmap.width,
                    reducedBitmap.height,
                    reducedBitmap.config
                )
                val smootherInput = Allocation.createFromBitmap(
                    rs,
                    reducedBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )
                val greyscaleTargetAllocation = Allocation.createFromBitmap(
                    rs,
                    greyscaleBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )

                // Inverts and greyscales the image
                colorIntrinsic.setGreyscale()
                colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
                greyscaleTargetAllocation.copyTo(greyscaleBitmap)

                // Run edge detection algorithm using a laplacian matrix convolution
                // Apply 3x3 convolution to detect edges
                val edgesBitmap = Bitmap.createBitmap(
                    reducedBitmap.width,
                    reducedBitmap.height,
                    reducedBitmap.config
                )
                val greyscaleInput = Allocation.createFromBitmap(
                    rs,
                    greyscaleBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )
                val edgesTargetAllocation = Allocation.createFromBitmap(
                    rs,
                    edgesBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )

                convolve.setInput(greyscaleInput)
                convolve.setCoefficients(laplacianMatrix)
                convolve.forEach(edgesTargetAllocation)
                edgesTargetAllocation.copyTo(edgesBitmap)

                // This is important to be false, otherwise image will be blank
                edgesBitmap.setHasAlpha(false)

                // Get standard deviation from meanStdDev
                meanStdDev(edgesBitmap)
            }
            return stdDev.await()
        }
    }

    /**
     * Finds the average of all pixels in the image
     * Also calculates the standard deviation from the average and pixel color
     * @param bitmap image after edge detection matrix application
     * @return stdDev variable with blurriness value
     * */
    private fun meanStdDev(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.height * bitmap.width)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var count = 0

        // When converted to greyscale, Red, Green and Blue have the same value
        var sumR = 0f

        for (pixel in pixels) {
            val r = Color.red(pixel)
            sumR += r
            if (r == 0) {
                count++
            }
        }

        val avgR = sumR / (pixels.size)

        var stdDevR = 0.0

        for (pixel in pixels) {
            val r = Color.red(pixel)
            stdDevR += (r - avgR).toDouble().pow(2.0)
        }

        return (sqrt(stdDevR / pixels.size) * 100).toFloat()
    }
}