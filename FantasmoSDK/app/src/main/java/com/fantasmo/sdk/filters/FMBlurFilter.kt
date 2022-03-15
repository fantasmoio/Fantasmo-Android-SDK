package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.YuvImage
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.utilities.MovingAverage
import kotlin.math.pow

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

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with ImageToBlurry failure
     */
    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        val yuvImage = fmFrame.yuvImage
            ?: return FMFrameFilterResult.Rejected(FMFrameFilterRejectionReason.FrameError)

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            histogram = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
            convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8(rs))
            convolve.setCoefficients(laplacianMatrix)
            resize = ScriptIntrinsicResize.create(rs)
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
            FMFrameFilterResult.Rejected(FMFrameFilterRejectionReason.ImageTooBlurry)
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
            val imageTypeBuilder = Type.Builder(rs, Element.U8(rs))
            imageTypeBuilder.setX(yuvImage.width)
            imageTypeBuilder.setY(yuvImage.height)
            val resizedImageTypeBuilder = Type.Builder(rs, Element.U8(rs))
            resizedImageTypeBuilder.setX(reducedWidth)
            resizedImageTypeBuilder.setY(reducedHeight)
            val inputAllocation = Allocation.createTyped(rs, imageTypeBuilder.create())
            val resizedAllocation = Allocation.createTyped(rs, resizedImageTypeBuilder.create())
            val convolveOutputAllocation = Allocation.createTyped(rs, resizedImageTypeBuilder.create())
            val binsAllocation = Allocation.createSized(rs, Element.U32(rs), 256)
            val histogramBins = IntArray(256)

            inputAllocation.copyFrom(yuvImage.yuvData)

            resize.setInput(inputAllocation)
            resize.forEach_bicubic(resizedAllocation)
            inputAllocation.destroy()

            convolve.setInput(resizedAllocation)
            convolve.forEach(convolveOutputAllocation)
            resizedAllocation.destroy()

            // Get standard deviation from meanStdDev
            histogram.setOutput(binsAllocation)
            histogram.forEach(convolveOutputAllocation)
            convolveOutputAllocation.destroy()

            binsAllocation.copyTo(histogramBins)
            binsAllocation.destroy()

            var avg = 0.0
            histogramBins.forEachIndexed { index, bin -> avg += (index * bin).toDouble() / (reducedWidth * reducedHeight).toDouble() }
            var stdDev = 0.0
            histogramBins.forEachIndexed { index, bin -> stdDev += (index - avg).pow(2.0) * bin / (reducedWidth * reducedHeight).toDouble()}
            return stdDev.toFloat()
        }
    }
}
