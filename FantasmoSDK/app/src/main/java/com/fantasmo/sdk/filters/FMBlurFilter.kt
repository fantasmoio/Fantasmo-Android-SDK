package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.MovingAverage
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
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
class FMBlurFilter(private val context: Context) : FMFrameFilter {

    private val TAG = "FMBlurFilter"

    private val laplacianMatrix = floatArrayOf(
        0.0f, 1.0f, 0.0f,
        1.0f, -4.0f, 1.0f,
        0.0f, 1.0f, 0.0f
    )

    private var variance: Double = 0.0
    private var varianceAverager = MovingAverage()
    private var averageVariance = varianceAverager.average

    private var varianceThreshold = 275.0
    private var suddenDropThreshold = 0.4

    private var throughputAverager = MovingAverage(8)
    private var averageThroughput: Double = throughputAverager.average

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooFast failure
     */
    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        GlobalScope.launch(Dispatchers.IO) { // launches coroutine in io thread
            variance = calculateVariance(arFrame)
        }
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
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.IMAGETOOBLURRY)
        } else {
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        }
    }

    /**
     * Calculates the variance using image convolution
     * Takes the frame and acquire the image from it and turns into greyscale
     * After that applies edge detection matrix to the greyscale image and
     * calculate variance from that
     * @param arFrame: frame to be measure the variance
     * @return variance: blurriness value
     * */
    suspend fun calculateVariance(arFrame: Frame): Double {
        try {
            val stdDev = GlobalScope.async {

                val cameraImage = arFrame.acquireCameraImage()

                val baOutputStream = FMUtility.createByteArrayOutputStream(cameraImage)

                // Release the image
                cameraImage.close()

                val imageBytes: ByteArray = baOutputStream.toByteArray()
                val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val rs = RenderScript.create(context)

                // Greyscale so we're only dealing with white <--> black pixels,
                // this is so we only need to detect pixel luminosity
                val greyscaleBitmap = Bitmap.createBitmap(
                    imageBitmap.width,
                    imageBitmap.height,
                    imageBitmap.config
                )
                val smootherInput = Allocation.createFromBitmap(
                    rs,
                    imageBitmap,
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
                val colorIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
                colorIntrinsic.setGreyscale()
                colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
                greyscaleTargetAllocation.copyTo(greyscaleBitmap)

                // Run edge detection algorithm using a laplacian matrix convolution
                // Apply 3x3 convolution to detect edges
                val edgesBitmap = Bitmap.createBitmap(
                    imageBitmap.width,
                    imageBitmap.height,
                    imageBitmap.config
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

                val convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
                convolve.setInput(greyscaleInput)
                convolve.setCoefficients(laplacianMatrix)
                convolve.forEach(edgesTargetAllocation)
                edgesTargetAllocation.copyTo(edgesBitmap)

                // This is important to be false, otherwise image will be blank
                edgesBitmap.setHasAlpha(false)
                val pixels = IntArray(edgesBitmap.height * edgesBitmap.width)
                edgesBitmap.getPixels(
                    pixels,
                    0,
                    edgesBitmap.width,
                    0,
                    0,
                    edgesBitmap.width,
                    edgesBitmap.height
                )
                Log.d(TAG, "Stopped Variance")

                // Get standard deviation from meanStdDev
                meanStdDev(edgesBitmap)
            }

            Log.i(TAG, "calculateVariance: ${stdDev.await()}")

            return stdDev.await()

        } catch (e: NotYetAvailableException) {
            Log.e(TAG, "FrameNotAvailable")
        }
        return 0.0
    }

    /**
     * Finds the average of all pixels in the image
     * Also calculates the standard deviation from the average and pixel color
     * @param bitmap: image after edge detection matrix application
     * @return stdDev: blurriness value
     * */
    private fun meanStdDev(bitmap: Bitmap): Double {
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

        return sqrt(stdDevR / pixels.size) * 100
    }
}