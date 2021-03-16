package com.example.fantasmo_android.screens.camera

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fantasmo_android.R
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment


class CameraFragment: Fragment() {

    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var currentView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.camera_fragment, container, false)
        return currentView
    }

    /**
     * To make any changes or get any values of the ArFragment always call onResume lifecycle
     *
     * */
    override fun onResume(){
        super.onResume()

        try{
            // Child Fragment Manager!!!
            arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arSceneView = arFragment.arSceneView
            val scene = arSceneView.scene
            scene.addOnUpdateListener { frameTime ->
                run {
                    arFragment.onUpdate(frameTime)
                    onUpdate()
                }
            }
            currentView.findViewById<Button>(R.id.checkParkingButton)?.setOnClickListener{
                Log.d("CheckPark Pressed", "CheckPark")
                Toast.makeText(this.activity, "CheckParking", Toast.LENGTH_SHORT)
            }

        }catch (e: Exception){
            Log.d("ArFragment Null", "ArFragment")
        }
    }

    /**
     * On any changes to the scene call onUpdate method to get arFrames and get the camera data
     * Data obtained from the sensor: Camera Translation and Camera Rotation values
     *
     * */
    private fun onUpdate() {
        val arFrame = arSceneView.arFrame

        val cameraTranslation = arFrame?.androidSensorPose?.translation
        currentView.findViewById<TextView>(R.id.cameraTranslation)?.text = createStringDisplay("Camera Translation: ", cameraTranslation)

        Log.d("Camera Translation", "CameraTranslation ${cameraTranslation.toString()}")

        val cameraRotation = arFrame?.androidSensorPose?.rotationQuaternion
        currentView.findViewById<TextView>(R.id.cameraAnglesText)?.text = createStringDisplay("Camera Rotation: ", cameraRotation)

        /*
        val arCamera = arFrame?.camera
        if (arCamera?.trackingState === TrackingState.TRACKING) {
            val cameraPose: Pose = arCamera.displayOrientedPose
            currentView.findViewById<TextView>(R.id.cameraTranslation)?.text = cameraPose.toString()
            Log.d("Camera Translation", "CameraTranslation $cameraPose")
        }*/
    }

    private fun createStringDisplay(s: String, cameraAttr: FloatArray?): CharSequence {
        return s + String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " + String.format("%.2f", cameraAttr?.get(2))
    }


}