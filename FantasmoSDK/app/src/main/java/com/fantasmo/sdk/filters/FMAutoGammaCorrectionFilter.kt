package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import android.renderscript.*
import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMAutoGammaCorrectionFilter(private val context: Context) : FMFrameFilter {

    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        var byteArrayFrame = FMUtility.acquireFrameImage(arFrame)
        GlobalScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            byteArrayFrame = applyAutoGammaCorrection(byteArrayFrame, 0.3f)
        }
        FMUtility.setFrame(byteArrayFrame)
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
        public suspend fun applyAutoGammaCorrection(byteArrayFrame: ByteArray?, meanT: Float): ByteArray? {
        if (byteArrayFrame == null) {
            return null
        } else {
            val autoGammaCorrection = GlobalScope.async {
                val rs = RenderScript.create(context)

                val luminanceInput = Allocation.createSized(rs, Element.U8(rs), FMUtility.imageWidth * FMUtility.imageHeight)
                luminanceInput.copyFrom(byteArrayFrame)
                val histogramIntrinsic = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
                val histogramOutput = Allocation.createSized(rs, Element.U32(rs), 256)
                histogramIntrinsic.setOutput(histogramOutput)
                histogramIntrinsic.forEach(luminanceInput)
                val histogram = IntArray(256)
                histogramOutput.copyTo(histogram)

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
                    byteArrayFrame
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
                    }
                    val colorLUT = ScriptIntrinsicLUT.create(rs, Element.U8_4(rs))
                    for (i in 0..255) {
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
                    val finalArray = byteArrayFrame.clone()
                    finalOutput.copyTo(finalArray)
                    finalArray
                }
            }
            return autoGammaCorrection.await()
        }
    }
}