package com.fantasmo.sdk.fantasmosdk

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.Request
import com.android.volley.ResponseDelivery
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.fantasmosdk.network.MockMultiPartRequest
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.fantasmosdk.utils.ImmediateResponseDelivery
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class FMLocationManagerTest : TestCase(){

    var instrumentationContext: Context = InstrumentationRegistry.getInstrumentation().context

    private val fmLocationManager : FMLocationManager = FMLocationManager(instrumentationContext.applicationContext)


    private var mDelivery: ResponseDelivery? = null
    private var mRequest: MockMultiPartRequest? = null
    private val url = "https://api.fantasmo.io/v1/parking.in.radius"
    private val radius = 10
    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    @Before
    @Throws(Exception::class)
    public override fun setUp() {
        mDelivery = ImmediateResponseDelivery()
        mRequest = MockMultiPartRequest(
            Request.Method.POST, url,
            {

            },
            {

            }
        )
        //MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testLocalize(){
        TODO("Not yet implemented")
    }

    @Test
    fun testIsZoneInRadius() {

        fmLocationManager.isConnected = false
        fmLocationManager.isSimulation = true
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius){
            assertEquals(it, true)
        }

        val radius2 = 100
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius2){
            assertEquals(it, false)
        }
    }

    @Test
    fun testAnchorDeltaPoseForFrame(){
        TODO("Not yet implemented")
    }
}