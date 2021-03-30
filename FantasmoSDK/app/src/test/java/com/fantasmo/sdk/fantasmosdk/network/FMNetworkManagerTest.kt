package com.fantasmo.sdk.fantasmosdk.network

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.*
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.LocalizeResponse
import com.fantasmo.sdk.network.FMNetworkManager
import com.fantasmo.sdk.volley.utils.CacheTestUtils
import com.fantasmo.sdk.volley.utils.ImmediateResponseDelivery
import com.google.gson.Gson
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class FMNetworkManagerTest : TestCase() {

    var instrumentationContext: Context = InstrumentationRegistry.getInstrumentation().context

    private val fmNetworkManager: FMNetworkManager =
        FMNetworkManager(FMConfiguration.getServerURL(), instrumentationContext)

    private val isSimulation = true
    private var currentLocation: android.location.Location = android.location.Location("")

    private var mSuccessResponse: Response<NetworkResponse>? = null

    private var mDelivery: ResponseDelivery? = null
    private var mRequest: MockMultiPartRequest? = null
    private val url = "https://api.fantasmo.io/v1/parking.in.radius"
    private val radius = 10
    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    @Before
    @Throws(Exception::class)
    public override fun setUp() {
        // Create Fake Service Responder
        mDelivery = ImmediateResponseDelivery()
        // Create Mock MultiPartRequest
        mRequest = MockMultiPartRequest(
            Request.Method.POST, url,
            { response ->
                val resultResponse = String(response.data)
                try {
                    Gson().fromJson(resultResponse, LocalizeResponse::class.java)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            {

            }
        )
        // Put mock request response in fake cache
        mRequest!!.sequence = 1
        val data = NetworkResponse(ByteArray(16))
        val cacheTest = CacheTestUtils()
        val cacheEntry: Cache.Entry = cacheTest.makeRandomCacheEntry(data)
        mSuccessResponse = Response.success(data, cacheEntry)
        //MockitoAnnotations.initMocks(this)
    }


    @Test
    fun testUploadImage() {
        // Make real request
        fmNetworkManager.uploadImage(
            ByteArray(16),
            getZoneInRadiusParams(radius),
            token,
            {
                println(it.location.toString())
            },
            {
                println(it)
            }
        )
        // Assert fake and real tests
        assertEquals(mRequest?.method, fmNetworkManager.multipartRequest.method)
    }

    @Test
    fun testZoneInRadiusRequest() {
        // Make real request
        fmNetworkManager.zoneInRadiusRequest(
            url,
            getZoneInRadiusParams(radius),
            token
        ) {

        }
        // Assert fake and real tests
        assertEquals(mRequest?.url, fmNetworkManager.multipartRequest.url)
        //assertEquals(mRequest?.headers, fmNetworkManager.multipartRequest.headers)
        assertEquals(mRequest?.method, fmNetworkManager.multipartRequest.method)
    }

    private fun getZoneInRadiusParams(radius: Int): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        val coordinates = if (isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        }

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(coordinates)

        return params
    }
}