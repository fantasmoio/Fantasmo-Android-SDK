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
    private val MAX_NUMBER_OF_LOOPS = 10

    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        val byteArrayFrame = FMUtility.acquireFrameImage(arFrame)
        var newByteArrayFrame : ByteArray?
        GlobalScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            newByteArrayFrame = applyAutoGammaCorrection(byteArrayFrame, 0.3f)
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

                val originalBitmap =
                    BitmapFactory.decodeByteArray(byteArrayFrame, 0, byteArrayFrame.size)
                val rs = RenderScript.create(context)

                // Greyscale so we're only dealing with white <--> black pixels,
                // this is so we only need to detect pixel luminosity
                val greyscaleBitmap = Bitmap.createBitmap(
                    originalBitmap.width,
                    originalBitmap.height,
                    originalBitmap.config
                )
                val bitmapInput = Allocation.createFromBitmap(
                    rs,
                    originalBitmap,
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
                colorIntrinsic.forEach(bitmapInput, greyscaleTargetAllocation)
                greyscaleTargetAllocation.copyTo(greyscaleBitmap)

                val histogramInput = Allocation.createFromBitmap(
                    rs,
                    greyscaleBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SHARED
                )
                val histogramIntrinsic = ScriptIntrinsicHistogram.create(rs, Element.U8(rs))
                val histogramOutput = Allocation.createSized(rs, Element.U32(rs), 256)
            histogramIntrinsic.setOutput(histogramOutput)
                histogramIntrinsic.forEach(histogramInput)
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
                        if(numOfLoops >= MAX_NUMBER_OF_LOOPS) {
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
                        colorLUT.setAlpha(i, i)
                    }
                    val finalInput = Allocation.createFromBitmap(
                        rs,
                        originalBitmap,
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SHARED
                    )
                    val finalBitmap = Bitmap.createBitmap(
                        originalBitmap.width,
                        originalBitmap.height,
                        originalBitmap.config
                    )
                    val finalOutput = Allocation.createFromBitmap(
                        rs,
                        finalBitmap,
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SHARED
                    )
                    colorLUT.forEach(finalInput, finalOutput)
                    val finalArray = ByteArray(finalOutput.bytesSize)
                    finalOutput.copyTo(finalArray)
                    finalArray
                }
            }
            return autoGammaCorrection.await()
        }
    }
}