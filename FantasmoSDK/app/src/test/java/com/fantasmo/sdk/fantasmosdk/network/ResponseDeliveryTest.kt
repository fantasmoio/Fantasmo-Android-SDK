package com.fantasmo.sdk.fantasmosdk.network

import android.os.Build
import com.android.volley.*
import com.fantasmo.sdk.fantasmosdk.utils.CacheTestUtils
import com.fantasmo.sdk.fantasmosdk.utils.ImmediateResponseDelivery
import com.fantasmo.sdk.mock.MockData
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.HashMap

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ResponseDeliveryTest {

    private var mDelivery: ExecutorDelivery? = null
    private var mRequest: MockMultiPartRequest? = null
    private var mSuccessResponse: Response<NetworkResponse>? = null

    private var respZoneIsInRadius : String = ""
    private var reqRespZoneIsInRadius : NetworkResponse? = null
    private var reqErrorZoneInRadiusResponse: VolleyError? = null

    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Make the delivery just run its posted responses immediately.
        mDelivery = ImmediateResponseDelivery()
        mockMultiPartRequest()
        mRequest!!.sequence = 1
        val data = NetworkResponse(ByteArray(16))
        val cacheTest = CacheTestUtils()
        val cacheEntry: Cache.Entry = cacheTest.makeRandomCacheEntry(data)
        mSuccessResponse = Response.success(data, cacheEntry)
    }

    @Test
    fun postResponseCallsDeliverResponse() {
        mDelivery!!.postResponse(mRequest, mSuccessResponse)
        mRequest?.let {
            TestCase.assertTrue(it.deliverResponseCalled)
        }
        mRequest?.let {
            TestCase.assertFalse(it.deliverErrorCalled)
        }
    }

    @Test
    fun postResponseSuppressesCanceled() {
        mRequest?.cancel()
        mDelivery!!.postResponse(mRequest, mSuccessResponse)
        mRequest?.let {
            TestCase.assertFalse(it.deliverResponseCalled)
        }
        mRequest?.let {
            TestCase.assertFalse(it.deliverErrorCalled)
        }
    }

    @Test
    fun postErrorCallsDeliverError() {
        val errorResponse: Response<ByteArray> = Response.error(ServerError())
        mDelivery!!.postResponse(mRequest, errorResponse)
        mRequest?.let {
            TestCase.assertTrue(it.deliverErrorCalled)
        }
        mRequest?.let {
            TestCase.assertFalse(it.deliverResponseCalled)
        }
    }

    private fun mockMultiPartRequest() {
        mRequest = object : MockMultiPartRequest(
            Method.POST, "https://api.fantasmo.io/v1/parking.in.radius",
            Response.Listener<NetworkResponse> { response ->
                reqRespZoneIsInRadius = response
                respZoneIsInRadius = String(response.data)
            },
            Response.ErrorListener {
                reqErrorZoneInRadiusResponse  = it
            }) {

            // Overriding getParams() to pass our parameters
            override fun getParams(): MutableMap<String, String> {
                return MockData.streetMockParameters()
            }

            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }
    }
}