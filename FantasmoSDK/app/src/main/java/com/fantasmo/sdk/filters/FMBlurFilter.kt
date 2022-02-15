package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.YuvImage
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.utilities.MovingAverage
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class responsible for filtering frames due to blur on images.
 * Prevents from sending blurred images.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
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

    private var varianceAverager = MovingAverage()
    private var averageVariance = varianceAverager.average

    private var varianceThreshold = blurFilterVarianceThreshold
    private var suddenDropThreshold = blurFilterSuddenDropThreshold
    private var averageThroughputThreshold = blurFilterAverageThroughputThreshold

    private var throughputAverager = MovingAverage(8)
    private var averageThroughput: Float = throughputAverager.average

    private val reducedHeight = 480
    private val reducedWidth = 640

    private lateinit var rs : RenderScript
    private lateinit var histogram: ScriptIntrinsicHistogram
    private lateinit var convolve : ScriptIntrinsicConvolve3x3
    private lateinit var resize : ScriptIntrinsicResize
    private lateinit var binsAllocation: Allocation
    private lateinit var resizedAllocation: Allocation
    private lateinit var convolveOutputAllocation: Allocation
    private var histogramBins = IntArray(256)

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with ImageToBlurry failure
     */
    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        val yuvImage = fmFrame.yuvImage
            ?: return FMFrameFilterResult.Rejected(FMFilterRejectionReason.FRAMEERROR)

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            histogram = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
            convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
            resize = ScriptIntrinsicResize.create(rs)
            val builder = Type.Builder(rs, Element.U8(rs))
            builder.setX(reducedWidth)
            builder.setY(reducedHeight)
            resizedAllocation = Allocation.createTyped(rs, builder.create())
            convolveOutputAllocation = Allocation.createTyped(rs, builder.create())
            binsAllocation = Allocation.createSized(rs, Element.U32(rs), 256)
            histogram.setOutput(binsAllocation)
        }

        val variance = calculateVariance(yuvImage)
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
            FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGETOOBLURRY)
        } else {
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
    fun calculateVariance(yuvImage: YuvImage?): Float {
        if (yuvImage == null) {
            return 0.0f
        } else {
            val builder = Type.Builder(rs, Element.U8(rs))
            builder.setX(yuvImage.width)
            builder.setY(yuvImage.height)
            val inputAllocation = Allocation.createTyped(rs, builder.create())

            inputAllocation.copyFrom(yuvImage.yuvData)

            resize.setInput(inputAllocation)
            resize.forEach_bicubic(resizedAllocation)
            inputAllocation.destroy()

            convolve.setInput(resizedAllocation)
            convolve.setCoefficients(laplacianMatrix)
            convolve.forEach(convolveOutputAllocation)

            // Get standard deviation from meanStdDev
            histogram.forEach(convolveOutputAllocation)

            binsAllocation.copyTo(histogramBins)
            var avg = 0.0
            histogramBins.forEachIndexed { index, bin -> avg += index * bin / (256.0 * convolveOutputAllocation.bytesSize) }
            var stdDev = 0.0
            histogramBins.forEachIndexed { index, bin -> stdDev += (index * bin / 256.0 - avg).pow(2.0) / convolveOutputAllocation.bytesSize}

            return (sqrt(stdDev) * 100.0).toFloat()
        }
    }
}
