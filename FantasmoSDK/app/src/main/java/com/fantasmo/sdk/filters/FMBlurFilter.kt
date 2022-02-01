package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.YuvImage
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
import kotlin.math.sqrt

/**
 * Class responsible for filtering frames due to blur on images.
 * Prevents from sending blurred images.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMBlurFilter(
    blurFilterVarianceThreshold: Float,
    blurFilterSuddenDropThreshold: Float,
    blurFilterAverageThroughputThreshold: Float,
    val context: Context
) : FMFrameFilter {
    override val TAG = FMBlurFilter::class.java.simpleName
    private val laplacianMatrix = floatArrayOf(
        0.0f, 1.0f, 0.0f,
        1.0f, -4.0f, 1.0f,
        0.0f, 1.0f, 0.0f
    )

    private var variance: Float = 0.0f
    private var varianceAverager = MovingAverage()
    private var averageVariance = varianceAverager.average

    private var varianceThreshold = blurFilterVarianceThreshold
    private var suddenDropThreshold = blurFilterSuddenDropThreshold
    private var averageThroughputThreshold = blurFilterAverageThroughputThreshold

    private var throughputAverager = MovingAverage(8)
    private var averageThroughput: Float = throughputAverager.average

    private lateinit var rs : RenderScript
    private lateinit var histogram: ScriptIntrinsicHistogram
    private lateinit var convolve : ScriptIntrinsicConvolve3x3

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with ImageToBlurry failure
     */
    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        val yuvImage = FMUtility.acquireFrameImage(arFrame)

        if(!::rs.isInitialized){
            rs = RenderScript.create(context)
            histogram = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
            convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
        }

        GlobalScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            variance = calculateVariance(yuvImage)
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
            FMUtility.setFrame(yuvImage)
            FMFrameFilterResult.Accepted
        }
    }

    /**
     * Calculates the variance using image convolution
     * Takes the frame and acquire the image from it and turns into greyscale
     * After that applies edge detection matrix to the greyscale image and
     * calculate variance from that
     * @param yuvImage frame converted to ByteArray to measure the variance
     * @return variance blurriness value
     * */
    suspend fun calculateVariance(yuvImage: YuvImage?): Float {
        val reducedHeight = 480
        val reducedWidth = 640
        if (yuvImage == null) {
            return 0.0f
        } else {
            val stdDev = GlobalScope.async {
                val inputBitmap = Bitmap.createBitmap(FMUtility.imageWidth, FMUtility.imageHeight, Bitmap.Config.ALPHA_8)
                val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap)
                inputAllocation.copyFrom(yuvImage.yuvData)
                inputAllocation.copyTo(inputBitmap)
                inputAllocation.destroy()
                val reducedBitmap =
                    Bitmap.createScaledBitmap(inputBitmap, reducedWidth, reducedHeight, true)

                // Run edge detection algorithm using a laplacian matrix convolution
                // Apply 3x3 convolution to detect edges
                val edgesBitmap = Bitmap.createBitmap(
                    reducedBitmap.width,
                    reducedBitmap.height,
                    reducedBitmap.config
                )
                val reducedInput = Allocation.createFromBitmap(
                    rs,
                    reducedBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )
                val edgesTargetAllocation = Allocation.createFromBitmap(
                    rs,
                    edgesBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )

                convolve.setInput(reducedInput)
                convolve.setCoefficients(laplacianMatrix)
                convolve.forEach(edgesTargetAllocation)
                edgesTargetAllocation.copyTo(edgesBitmap)
                reducedInput.destroy()
                edgesTargetAllocation.destroy()
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
        val inputAllocation = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED)
        val bins = IntArray(256)
        val binsAllocation = Allocation.createSized(rs, Element.U32(rs), 256)
        histogram.setOutput(binsAllocation)
        histogram.forEach(inputAllocation)
        binsAllocation.copyTo(bins)
        inputAllocation.destroy()
        binsAllocation.destroy()
        var avg = 0.0
        bins.forEachIndexed { index, bin -> avg += index * bin / (256.0 * bitmap.byteCount) }
        var stdDev = 0.0
        bins.forEachIndexed { index, bin -> stdDev += (index * bin / (256.0 * bitmap.byteCount)) - avg}
        return (sqrt(stdDev) * 100.0).toFloat()
    }
}
