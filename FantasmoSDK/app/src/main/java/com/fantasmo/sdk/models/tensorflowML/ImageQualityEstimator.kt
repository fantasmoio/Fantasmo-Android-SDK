package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.os.Build
import com.google.ar.core.Frame

class ImageQualityEstimator {

    companion object{
        fun makeEstimator(context: Context): ImageQualityEstimatorProtocol{
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ImageQualityEstimatorMLKit(context)
            } else {
                ImageQualityEstimatorDeviceNotSupported()
            }
        }
    }
}

interface ImageQualityEstimatorProtocol{
    fun estimateImageQuality(frame:Frame):ImageQualityEstimationResult
    fun estimateImageQuality(frame:Frame, callback: (ImageQualityEstimationResult) -> Unit)
}

class ImageQualityEstimatorDeviceNotSupported : ImageQualityEstimatorProtocol{
    override fun estimateImageQuality(frame: Frame) : ImageQualityEstimationResult{
        return ImageQualityEstimationResult.ERROR("Device Not Supported")
    }

    override fun estimateImageQuality(
        frame: Frame,
        callback: (ImageQualityEstimationResult) -> Unit
    ) {
        callback(ImageQualityEstimationResult.ERROR("Device Not Supported"))
        return
    }
}