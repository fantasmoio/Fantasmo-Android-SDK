package com.fantasmo.sdk.fantasmosdk.network

import android.os.Build
import com.android.volley.*
import com.fantasmo.sdk.volley.utils.CacheTestUtils
import com.fantasmo.sdk.volley.utils.ImmediateResponseDelivery
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ResponseDeliveryTest {

    private var mDelivery: ExecutorDelivery? = null
    private var mRequest: MockMultiPartRequest? = null
    private var mSuccessResponse: Response<NetworkResponse>? = null
    private val url = "https://api.fantasmo.io/v1/parking.in.radius"

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Make the delivery just run its posted responses immediately.
        mDelivery = ImmediateResponseDelivery()
        mRequest = MockMultiPartRequest(
            Request.Method.POST, url,
            {
                println(it.allHeaders)
            },
            {
                println(it)
            }
        )
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
}