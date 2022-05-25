package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.YuvImage
import android.os.Build
import androidx.annotation.RequiresApi
import android.renderscript.*
import com.fantasmo.sdk.models.FMFrame
import kotlin.math.pow
/**
 * Class responsible for correcting image brightness
 * Prevents from sending images that are too dark
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMImageEnhancer(private val targetBrightness : Float, private val context: Context) {
    val TAG = FMImageEnhancer::class.java.simpleName

    private lateinit var rs : RenderScript
    private lateinit var histogramIntrinsic: ScriptIntrinsicHistogram
    private lateinit var colorLUT : ScriptIntrinsicLUT

    fun enhance(fmFrame: FMFrame) {
        if(!::rs.isInitialized){
            rs = RenderScript.create(context)
            histogramIntrinsic = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
            colorLUT = ScriptIntrinsicLUT.create(rs, Element.U8_4(rs))
        }
        applyAutoGammaCorrection(fmFrame, targetBrightness)
    }


    /**
     * Gamma correction process based on histogram
     * Takes the luminance channel of the input image and calculates its histogram
     * Runs a loop to determine the needed gamma for correction, then applies it to the image
     * @param yuvImage YUV image from the frame to be corrected
     * @param meanT Target brightness
     * @return Corrected YUV image
     * */
    private fun applyAutoGammaCorrection(fmFrame: FMFrame, meanT: Float) {
        val yuvImage = fmFrame.yuvImage
        if (yuvImage == null) {
            return
        } else {
            // Calculate histogram with RenderScript
            val luminanceInput = Allocation.createSized(rs, Element.U8(rs), yuvImage.height * yuvImage.width)
            luminanceInput.copyFrom(yuvImage.yuvData)
            val histogramOutput = Allocation.createSized(rs, Element.U32(rs), 256)
            histogramIntrinsic.setOutput(histogramOutput)
            histogramIntrinsic.forEach(luminanceInput)
            val histogram = IntArray(256)
            histogramOutput.copyTo(histogram)

            luminanceInput.destroy()
            histogramOutput.destroy()

            // Assign float bin values, then calculate mean brightness
            val originalBinValues = DoubleArray(256)
            for (i in 0..255) {
                originalBinValues[i] = i / 256.0
            }
            val correctedBinValues = originalBinValues.clone()
            val histogramSum = histogram.sum()
            var meanBrightness = 0.0
            for (i in 0..255) {
                meanBrightness += originalBinValues[i] * histogram[i] / histogramSum
            }

            if (meanBrightness > meanT) {
                return
            }
            else {
                // Trying to fit mean brightness around meanT +- 1%
                val meanRange: DoubleArray = doubleArrayOf(meanT - meanT / 100.0, meanT + meanT / 100.0)

                var gamma = 1.0
                var step = 0.5
                var numOfLoops = 0
                // Loop that decimates gamma value between 0 and 1, dividing step by 2 every time until we hone in on a valid gamma value
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
                    // Recalculating mean brightness before next check
                    for (i in 0..255) {
                        correctedBinValues[i] = originalBinValues[i]
                        correctedBinValues[i] = correctedBinValues[i].pow(gamma)
                        meanBrightness += correctedBinValues[i] * histogram[i] / histogramSum
                    }
                }

                // Applying new gamma to Y channel via the color LUT intrinsic. We trick the intrinsic in thinking we're processing an RGBA image
                val finalBins = IntArray(256)
                for (i in 0..255) {
                    finalBins[i] = (correctedBinValues[i] * 256.0).toInt()
                    colorLUT.setBlue(i, finalBins[i])
                    colorLUT.setRed(i, finalBins[i])
                    colorLUT.setGreen(i, finalBins[i])
                    colorLUT.setAlpha(i, finalBins[i])
                }

                val finalInput = Allocation.createSized(rs, Element.U8_4(rs), yuvImage.height * yuvImage.width / 4)
                finalInput.copyFrom(yuvImage.yuvData)
                val finalOutput = Allocation.createSized(
                    rs, Element.U8_4(rs), yuvImage.width * yuvImage.height / 4
                )
                colorLUT.forEach(finalInput, finalOutput)
                val outputArray = ByteArray(yuvImage.height * yuvImage.width)
                finalOutput.copyTo(outputArray)

                // Copying corrected values to start of the output array, and UV from original image to the end, then creating new YuvImage
                val finalArray = ByteArray(yuvImage.yuvData.size)
                outputArray.copyInto(finalArray, 0, 0, outputArray.size)
                yuvImage.yuvData.copyInto(finalArray, yuvImage.width * yuvImage.height, yuvImage.width * yuvImage.height, yuvImage.yuvData.size - 1)
                finalInput.destroy()
                finalOutput.destroy()
                fmFrame.enhancedImageGamma = gamma.toFloat()
                fmFrame.yuvImage = YuvImage(finalArray, yuvImage.yuvFormat, yuvImage.width, yuvImage.height, yuvImage.strides)
            }
        }
    }
}
