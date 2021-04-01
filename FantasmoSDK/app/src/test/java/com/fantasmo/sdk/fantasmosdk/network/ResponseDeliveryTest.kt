package com.fantasmo.sdk.fantasmosdk.network

import com.android.volley.*
import com.fantasmo.sdk.fantasmosdk.utils.CacheTestUtils
import com.fantasmo.sdk.fantasmosdk.utils.ImmediateResponseDelivery
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.network.MultiPartRequest
import com.google.gson.Gson
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*


class ResponseDeliveryTest {

    private lateinit var mDelivery: ExecutorDelivery
    private lateinit var mRequest: MultiPartRequest
    private lateinit var mSuccessResponse: Response<NetworkResponse>

    private val token = "API_KEY"

    @Before
    fun setUp() {
        // Make the delivery just run its posted responses immediately.
        mDelivery = ImmediateResponseDelivery()
        mockMultiPartRequest()
        mRequest.sequence = 1

        val localizeResponseMock = createLocalizeResponse()
        val jsonResponse = Gson().toJson(localizeResponseMock)
        val networkResponse = NetworkResponse(jsonResponse.toByteArray())
        val cacheTest = CacheTestUtils()
        val cacheEntry: Cache.Entry = cacheTest.makeRandomCacheEntry(networkResponse)
        mSuccessResponse = Response.success(networkResponse, cacheEntry)
    }

    @Test
    fun postResponseCallsDeliverResponse() {
        mDelivery.postResponse(mRequest, mSuccessResponse)
        assertTrue(mRequest.deliverResponseCalled)
        assertFalse(mRequest.deliverErrorCalled)
    }

    @Test
    fun postResponseSuppressesCanceled() {
        mRequest.cancel()
        mDelivery.postResponse(mRequest, mSuccessResponse)
        assertFalse(mRequest.deliverResponseCalled)
        assertFalse(mRequest.deliverErrorCalled)
    }

    @Test
    fun postErrorCallsDeliverError() {
        val errorResponse: Response<ByteArray> = Response.error(ServerError())
        mDelivery.postResponse(mRequest, errorResponse)
        assertTrue(mRequest.deliverErrorCalled)
        assertFalse(mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverTimeoutError() {
        val errorResponse: Response<ByteArray> = Response.error(TimeoutError())
        mDelivery.postResponse(mRequest, errorResponse)
        assertTrue(mRequest.deliverErrorCalled)
        assertFalse(mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverNoConnectionError() {
        val errorResponse: Response<ByteArray> = Response.error(NoConnectionError())
        mDelivery.postResponse(mRequest, errorResponse)
        assertTrue(mRequest.deliverErrorCalled)
        assertFalse(mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverResourceNotFoundError() {
        val error = ErrorResponse(404,"Resource not found")
        val jsonResponse = Gson().toJson(error)
        val volleyError = VolleyError(jsonResponse)
        val errorResponse: Response<ByteArray> = Response.error(volleyError)
        mDelivery.postResponse(mRequest, errorResponse)
        assertEquals(true, mRequest.deliverErrorCalled)
        assertEquals(false, mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverAuthenticationError() {
        val error = ErrorResponse(401,"Authentication error")
        val jsonResponse = Gson().toJson(error)
        val volleyError = VolleyError(jsonResponse)
        val errorResponse: Response<ByteArray> = Response.error(volleyError)
        mDelivery.postResponse(mRequest, errorResponse)
        assertEquals(true, mRequest.deliverErrorCalled)
        assertEquals(false, mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverImageNotValidError() {
        val error = ErrorResponse(400,"Query image is not a valid JPEG")
        val jsonResponse = Gson().toJson(error)
        val volleyError = VolleyError(jsonResponse)
        val errorResponse: Response<ByteArray> = Response.error(volleyError)
        mDelivery.postResponse(mRequest, errorResponse)
        assertEquals(true, mRequest.deliverErrorCalled)
        assertEquals(false, mRequest.deliverResponseCalled)
    }

    @Test
    fun postErrorCallsDeliverOutOfMapError() {
        val error = ErrorResponse(400,"map not found! lat=48.848138681935886, lon=4.371750713292894 is not in any map.")
        val jsonResponse = Gson().toJson(error)
        val volleyError = VolleyError(jsonResponse)
        val errorResponse: Response<ByteArray> = Response.error(volleyError)
        mDelivery.postResponse(mRequest, errorResponse)
        assertEquals(true, mRequest.deliverErrorCalled)
        assertEquals(false, mRequest.deliverResponseCalled)
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

    private fun mockMultiPartRequest() {
        mRequest = object : MultiPartRequest(
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
}