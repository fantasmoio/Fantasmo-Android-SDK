package com.fantasmo.sdk.fantasmosdk.network

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.*
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.mock.MockData.Companion.getFileDataFromDrawable
import com.fantasmo.sdk.network.FMNetworkManager
import com.fantasmo.sdk.fantasmosdk.utils.CacheTestUtils
import com.fantasmo.sdk.fantasmosdk.utils.ImmediateResponseDelivery
import com.fantasmo.sdk.models.*
import com.google.gson.Gson
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class FMNetworkManagerTest {

    private lateinit var instrumentationContext: Context
    private lateinit var fmNetworkManager: FMNetworkManager

    private val isSimulation = true
    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    private lateinit var mDelivery: ResponseDelivery

    // ZoneIsInRadius MockRequest
    private lateinit var reqZoneIsInRadius: MockMultiPartRequest
    // UploadImage MockRequest
    private lateinit var reqUploadImage: MockMultiPartRequest

    private lateinit var cacheTest: CacheTestUtils

    @Before
    fun setUp() {
        initMocks(this)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
            //Mockito.mock(Context::class.java)
        fmNetworkManager = FMNetworkManager(FMConfiguration.getServerURL(), instrumentationContext)
        // Create Fake Service Responder
        mDelivery = ImmediateResponseDelivery()
        // Create Mock MultiPartRequest
        mockMultiPartRequestZoneIsInRadius()
        mockMultiPartRequestUploadImage()

        // Create fake cache
        cacheTest = CacheTestUtils()
    }


    @Test
    fun testUploadImage() {
        val radius = 10
        // Make real request
        fmNetworkManager.uploadImage(
            getFileDataFromDrawable(
                BitmapFactory.decodeResource(instrumentationContext.resources, R.drawable.image_on_street)),
            getZoneInRadiusParams(radius),
            token,
            {
            },
            {
            }
        )
        // Assert fake and real requests
        assertNotNull(reqUploadImage)
        assertNotNull(fmNetworkManager.multipartRequest)

        // Assert fake and real request construction
        assertEquals(reqUploadImage.url, fmNetworkManager.multipartRequest.url)
        assertEquals(reqUploadImage.method, fmNetworkManager.multipartRequest.method)
        assertEquals(reqUploadImage.headers, fmNetworkManager.multipartRequest.headers)
        assertEquals(reqUploadImage.priority, fmNetworkManager.multipartRequest.priority)

        postResponseRequest()
        // Assert if response was delivered without errors
        assertTrue(fmNetworkManager.multipartRequest.deliverResponseCalled)
        assertFalse(fmNetworkManager.multipartRequest.deliverErrorCalled)
    }

    @Test
    fun testZoneInRadiusRequest() {
        val radius = 10
        // Make real request
        fmNetworkManager.zoneInRadiusRequest(
            "https://api.fantasmo.io/v1/parking.in.radius",
            getZoneInRadiusParams(radius),
            token
        ) {
        }
        // Assert fake and real requests
        assertNotNull(reqZoneIsInRadius)
        assertNotNull(fmNetworkManager.multipartRequest)

        // Assert fake and real request construction
        assertEquals(reqZoneIsInRadius.url, fmNetworkManager.multipartRequest.url)
        assertEquals(reqZoneIsInRadius.method, fmNetworkManager.multipartRequest.method)
        assertEquals(reqZoneIsInRadius.headers, fmNetworkManager.multipartRequest.headers)

        postResponseRequest()
        // Assert if response was delivered without errors
        assertTrue(fmNetworkManager.multipartRequest.deliverResponseCalled)
        assertFalse(fmNetworkManager.multipartRequest.deliverErrorCalled)
    }

    private fun mockMultiPartRequestUploadImage() {
        reqUploadImage = object : MockMultiPartRequest(
            Method.POST, "https://api.fantasmo.io/v1/image.localize",
            {
            },
            {
            }) {
            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }
    }

    private fun mockMultiPartRequestZoneIsInRadius() {
        reqZoneIsInRadius = object : MockMultiPartRequest(
            Method.POST, "https://api.fantasmo.io/v1/parking.in.radius",
            {
            },
            {
            }) {
            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }
    }

    private fun getZoneInRadiusParams(radius: Int): HashMap<String, String> {
        val params = hashMapOf<String, String>()
        val currentLocation: android.location.Location = android.location.Location("")

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

    private fun createLocalizeResponse(): LocalizeResponse {
        val coordinate = Coordinate(48.84972140031428, 2.3726263972863566)
        val location = Location(null, coordinate, null, null, null, null)
        val pose = Pose(
            "N/A",
            Orientation(
                0.8418958187103271,
                0.03637034818530083,
                0.5383867025375366,
                -0.005325936246663332
            ),
            Position(-0.9883674383163452, -0.9312995672225952, 0.6059572100639343)
        )
        return LocalizeResponse(null, location, pose, "30989ac2-b7c7-4619-b078-04e669a13937")
    }

    private fun postResponseRequest() {
        val localizeResponseMock = createLocalizeResponse()
        val jsonResponse = Gson().toJson(localizeResponseMock)
        val networkResponse = NetworkResponse(jsonResponse.toByteArray())
        val cacheEntry: Cache.Entry = cacheTest.makeRandomCacheEntry(networkResponse)
        val successResponse = Response.success(networkResponse, cacheEntry)
        mDelivery.postResponse(fmNetworkManager.multipartRequest, successResponse)
    }
}