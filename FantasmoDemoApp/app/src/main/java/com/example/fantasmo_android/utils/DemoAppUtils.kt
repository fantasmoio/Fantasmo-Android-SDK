package com.example.fantasmo_android.utils

class DemoAppUtils {

    object AppUtils{
        fun createStringDisplay(s: String, cameraAttr: FloatArray?): String {
            return s + String.format("%.2f", cameraAttr?.get(0)) + ", " +
                    String.format("%.2f", cameraAttr?.get(1)) + ", " + String.format("%.2f", cameraAttr?.get(2))
        }
    }

}