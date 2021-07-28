package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame

class AccumulatedARCoreInfo {

    private var translationAccumulator = TotalDeviceTranslationAccumulator(10)
    private var rotationAccumulator = TotalDeviceRotationAccumulator()

    fun reset(){
        translationAccumulator.reset()
        rotationAccumulator.reset()
    }

    fun update(arFrame: Frame) {
        translationAccumulator.update(arFrame)
        rotationAccumulator.update(arFrame)
    }

}