package com.fantasmo.sdk.network

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.Cache
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.ResponseDelivery
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.utils.CacheTestUtils
import com.fantasmo.sdk.utils.ImmediateResponseDelivery
import com.fantasmo.sdk.mock.MockData.Companion.getFileDataFromDrawable
import com.fantasmo.sdk.models.*
import com.google.gson.Gson
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.openMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*
import kotlin.collections.HashMap

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class FMNetworkManagerTest {

    private lateinit var instrumentationContext: Context
    private lateinit var fmNetworkManager: FMNetworkManager

    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    private lateinit var mDelivery: ResponseDelivery

    private lateinit var reqZoneIsInRadius: MockMultiPartRequest

    private lateinit var reqUploadImage: MockMultiPartRequest

    private lateinit var cacheTest: CacheTestUtils

    private lateinit var location: Location
    private lateinit var coordinate: Coordinate
    private lateinit var pose: Pose

    @Before
    fun setUp() {
        openMocks(this)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context

        fmNetworkManager = FMNetworkManager(FMConfiguration.getServerURL(), instrumentationContext)

        mDelivery = ImmediateResponseDelivery()

        mockMultiPartRequestZoneIsInRadius()
        mockMultiPartRequestUploadImage()

        coordinate = Coordinate(48.84972140031428, 2.3726263972863566)
        location = Location(null, coordinate, null, null, null, null)
        pose = Pose(
            "N/A",
            Orientation(
                0.8418958187103271,
                0.03637034818530083,
                0.5383867025375366,
                -0.005325936246663332
            ),
            Position(-0.9883674383163452, -0.9312995672225952, 0.6059572100639343)
        )

        cacheTest = CacheTestUtils()
    }

    @Test
    fun testUploadImage() {
        fmNetworkManager.uploadImage(
            getFileDataFromDrawable(
                BitmapFactory.decodeResource(
                    instrumentationContext.resources,
                    R.drawable.image_on_street
                )
            ),
            getLocalizeParams(),
            token,
            {
                assertEquals(coordinate.latitude, it.location?.coordinate?.latitude)
                assertEquals(coordinate.longitude, it.location?.coordinate?.longitude)

                assertEquals(pose.orientation.w, it.pose?.orientation?.w)
                assertEquals(pose.orientation.x, it.pose?.orientation?.x)
                assertEquals(pose.orientation.y, it.pose?.orientation?.y)
                assertEquals(pose.orientation.z, it.pose?.orientation?.z)
            },
            {
            }
        )
        assertNotNull(reqUploadImage)
        assertNotNull(fmNetworkManager.multipartRequest)

        assertEquals(reqUploadImage.url, fmNetworkManager.multipartRequest.url)
        assertEquals(reqUploadImage.method, fmNetworkManager.multipartRequest.method)
        assertEquals(reqUploadImage.headers, fmNetworkManager.multipartRequest.headers)
        assertEquals(reqUploadImage.priority, fmNetworkManager.multipartRequest.priority)

        postResponseRequest()
        assertTrue(fmNetworkManager.multipartRequest.deliverResponseCalled)
        assertFalse(fmNetworkManager.multipartRequest.deliverErrorCalled)
    }

    @Test
    fun testZoneInRadiusRequest() {
        fmNetworkManager.zoneInRadiusRequest(
            "https://api.fantasmo.io/v1/parking.in.radius",
            getZoneInRadiusParams(10),
            token
        ) {
        }
        assertNotNull(reqZoneIsInRadius)
        assertNotNull(fmNetworkManager.multipartRequest)

        assertEquals(reqZoneIsInRadius.url, fmNetworkManager.multipartRequest.url)
        assertEquals(reqZoneIsInRadius.method, fmNetworkManager.multipartRequest.method)
        assertEquals(reqZoneIsInRadius.headers, fmNetworkManager.multipartRequest.headers)
        assertEquals(reqZoneIsInRadius.priority, fmNetworkManager.multipartRequest.priority)

        postResponseRequest()
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
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }
    }

    private fun getZoneInRadiusParams(radius: Int): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(Coordinate(48.84972140031428, 2.3726263972863566))

        return params
    }

    private fun createLocalizeResponse(): LocalizeResponse {
        return LocalizeResponse(null, location, pose, "30989ac2-b7c7-4619-b078-04e669a13937")
    }

    private fun getLocalizeParams(): HashMap<String, String> {
        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = System.currentTimeMillis().toString()
        params["gravity"] = gson.toJson(
            Orientation(
                0.8418958187103271,
                0.03637034818530083,
                0.5383867025375366,
                -0.005325936246663332
            )
        )
        params["uuid"] = UUID.randomUUID().toString()
        params["coordinate"] = gson.toJson(Coordinate(48.84972140031428, 2.3726263972863566))
        params["intrinsics"] = gson.toJson(
            FMIntrinsics(
                1083.401611328125f,
                1083.401611328125f,
                481.0465087890625f,
                629.142822265625f
            )
        )

        return params
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