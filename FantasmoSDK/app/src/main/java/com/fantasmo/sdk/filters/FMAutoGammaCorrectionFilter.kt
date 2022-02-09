package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.YuvImage
import android.os.Build
import androidx.annotation.RequiresApi
import android.renderscript.*
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMAutoGammaCorrectionFilter(private val context: Context) : FMFrameFilter {
    override val TAG = FMAutoGammaCorrectionFilter::class.java.simpleName

    private lateinit var rs : RenderScript
    private lateinit var histogramIntrinsic: ScriptIntrinsicHistogram
    private lateinit var colorLUT : ScriptIntrinsicLUT

    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        if(!::rs.isInitialized){
            rs = RenderScript.create(context)
            histogramIntrinsic = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
            colorLUT = ScriptIntrinsicLUT.create(rs, Element.U8_4(rs))
        }
        fmFrame.processedYuvImage = applyAutoGammaCorrection(fmFrame.yuvImage, 0.3f)
        return FMFrameFilterResult.Accepted
    }


    /**
     * Calculates the variance using image convolution
     * Takes the frame and acquire the image from it and turns into greyscale
     * After that applies edge detection matrix to the greyscale image and
     * calculate variance from that
     * @param byteArrayFrame frame converted to ByteArray to measure the variance
     * @return variance blurriness value
     * */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun applyAutoGammaCorrection(yuvImage: YuvImage, meanT: Float): YuvImage {
        val luminanceInput = Allocation.createSized(rs, Element.U8(rs), FMUtility.imageWidth * FMUtility.imageHeight)
        luminanceInput.copyFrom(yuvImage.yuvData)
        val histogramOutput = Allocation.createSized(rs, Element.U32(rs), 256)
        histogramIntrinsic.setOutput(histogramOutput)
        histogramIntrinsic.forEach(luminanceInput)
        val histogram = IntArray(256)
        histogramOutput.copyTo(histogram)

        luminanceInput.destroy()
        histogramOutput.destroy()

        val originalBinValues = DoubleArray(256)
        for (i in 0..255) {
            originalBinValues[i] = i / 256.0
        }
        var correctedBinValues = originalBinValues.clone()
        val histogramSum = histogram.sum()
        var meanBrightness = 0.0
        for (i in 0..255) {
            meanBrightness += originalBinValues[i] * histogram[i] / histogramSum
        }

        if (meanBrightness > meanT) {
            return yuvImage
        }
        else {
            val meanRange: DoubleArray = doubleArrayOf(meanT - meanT / 100.0, meanT + meanT / 100.0)

            var gamma = 1.0
            var step = 0.5
            var numOfLoops = 0

            while (true) {
                numOfLoops++
                if (meanBrightness >= meanRange[0] && meanBrightness <= meanRange[1]) {
                    break
                }
                if (meanBrightness < meanT) {
                    gamma -= step
                } else {
                    gamma += step
                }
                step /= 2.0
                meanBrightness = 0.0
                for (i in 0..255) {
                    correctedBinValues[i] = originalBinValues[i]
                    correctedBinValues[i] = correctedBinValues[i].pow(gamma)
                    meanBrightness += correctedBinValues[i] * histogram[i] / histogramSum
                }
            }
            var finalBins = IntArray(256)
            for (i in 0..255) {
                finalBins[i] = (correctedBinValues[i] * 256.0).toInt()
                colorLUT.setBlue(i, finalBins[i])
                colorLUT.setRed(i, finalBins[i])
                colorLUT.setGreen(i, finalBins[i])
                colorLUT.setAlpha(i, finalBins[i])
            }
            val finalInput = Allocation.createSized(rs, Element.U8_4(rs), FMUtility.imageWidth * FMUtility.imageHeight / 4)
            val finalOutput = Allocation.createSized(
                rs, Element.U8_4(rs), FMUtility.imageWidth * FMUtility.imageHeight / 4
            )
            colorLUT.forEach(finalInput, finalOutput)
            val finalArray = ByteArray(yuvImage.yuvData.size)
            finalOutput.copyTo(finalArray)
            yuvImage.yuvData.copyInto(finalArray, yuvImage.width * yuvImage.height, yuvImage.width * yuvImage.height, yuvImage.yuvData.size - 1)
            return YuvImage(finalArray, yuvImage.yuvFormat, yuvImage.width, yuvImage.height, yuvImage.strides)
        }
    }
}