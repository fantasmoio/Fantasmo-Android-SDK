package com.fantasmo.sdk.fantasmosdk.network

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.*
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.mock.MockData.Companion.getFileDataFromDrawable
import com.fantasmo.sdk.mock.MockData.Companion.streetMockParameters
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.network.FMNetworkManager
import com.fantasmo.sdk.network.FileDataPart
import com.fantasmo.sdk.fantasmosdk.utils.CacheTestUtils
import com.fantasmo.sdk.fantasmosdk.utils.ImmediateResponseDelivery
import com.fantasmo.sdk.models.LocalizeResponse
import com.google.gson.Gson
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class FMNetworkManagerTest : TestCase() {

    var instrumentationContext: Context = InstrumentationRegistry.getInstrumentation().context

    //@Mock
    //private val context: Context = Mockito.mock(Context::class.java)
    private val fmNetworkManager: FMNetworkManager =
        FMNetworkManager(FMConfiguration.getServerURL(), instrumentationContext)

    private val isSimulation = true
    private var currentLocation: android.location.Location = android.location.Location("")

    private var mSuccessResponse: Response<NetworkResponse>? = null
    private var mSuccessResponse2: Response<NetworkResponse>? = null

    private var mDelivery: ResponseDelivery? = null

    // ZoneIsInRadius MockRequest
    private var reqZoneIsInRadius: MockMultiPartRequest? = null
    private var respZoneIsInRadius : String = ""
    private var reqRespZoneIsInRadius : NetworkResponse? = null
    private var reqErrorZoneInRadiusResponse: VolleyError? = null

    // UploadImage MockRequest
    private var reqUploadImage: MockMultiPartRequest? = null
    private var respUploadImage : String = ""
    private var reqRespUploadImage: NetworkResponse? = null
    private var reqErrorUploadImage : VolleyError? = null

    private val radius = 10
    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    private var cacheTest: CacheTestUtils? = null

    @Before
    @Throws(Exception::class)
    public override fun setUp() {
        initMocks(this)
        // Create Fake Service Responder
        mDelivery = ImmediateResponseDelivery()
        // Create Mock MultiPartRequest
        mockMultiPartRequestZoneIsInRadius()
        mockMultiPartRequestUploadImage()

        // Put mock request response in fake cache
        reqZoneIsInRadius!!.sequence = 1
        reqUploadImage!!.sequence = 1
        cacheTest = CacheTestUtils()
        val cacheEntry1: Cache.Entry = cacheTest!!.makeRandomCacheEntry(reqRespZoneIsInRadius)
        val cacheEntry2: Cache.Entry = cacheTest!!.makeRandomCacheEntry(reqRespUploadImage)
        mSuccessResponse = Response.success(reqRespZoneIsInRadius, cacheEntry1)
        mSuccessResponse2 = Response.success(reqRespUploadImage, cacheEntry2)
    }


    @Test
    fun testUploadImage() {
        // Make real request
        fmNetworkManager.uploadImage(
            getFileDataFromDrawable(
                BitmapFactory.decodeResource(instrumentationContext.resources, R.drawable.image_on_street)),
            getZoneInRadiusParams(radius),
            token,
            {
                Response.Listener<NetworkResponse> { response ->
                    val resultResponse = String(response.data)
                    try {
                        val localizeResult =
                            Gson().fromJson(resultResponse, LocalizeResponse::class.java)
                        //onCompletion(localizeResult)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            },
            {

            }
        )
        // Assert fake and real requests
        //assertNotNull(reqUploadImage)
        //assertNotNull(fmNetworkManager.multipartRequest)
//
        //// Assert fake and real request construction
        //assertEquals(reqUploadImage?.url, fmNetworkManager.multipartRequest.url)
        //assertEquals(reqUploadImage?.method, fmNetworkManager.multipartRequest.method)
        //assertEquals(reqUploadImage?.headers, fmNetworkManager.multipartRequest.headers)
        //assertEquals(reqUploadImage?.priority, fmNetworkManager.multipartRequest.priority)

        val localizeResponse = Gson().toJson("{\n" +
                "    \"geofences\": null,\n" +
                "    \"location\": {\n" +
                "        \"altitude\": null,\n" +
                "        \"coordinate\": {\n" +
                "            \"latitude\": 48.84972140031428,\n" +
                "            \"longitude\": 2.3726263972863566\n" +
                "        },\n" +
                "        \"floor\": null,\n" +
                "        \"heading\": null,\n" +
                "        \"horizontalAccuracy\": null,\n" +
                "        \"verticalAccuracy\": null\n" +
                "    },\n" +
                "    \"pose\": {\n" +
                "        \"accuracy\": \"N/A\",\n" +
                "        \"orientation\": {\n" +
                "            \"w\": 0.8418958187103271,\n" +
                "            \"x\": 0.03637034818530083,\n" +
                "            \"y\": 0.5383867025375366,\n" +
                "            \"z\": -0.005325936246663332\n" +
                "        },\n" +
                "        \"position\": {\n" +
                "            \"x\": -0.9883674383163452,\n" +
                "            \"y\": -0.9312995672225952,\n" +
                "            \"z\": 0.6059572100639343\n" +
                "        }\n" +
                "    },\n" +
                "    \"uuid\": \"30989ac2-b7c7-4619-b078-04e669a13937\"\n" +
                "}")
        val networkResponse = NetworkResponse(localizeResponse.toByteArray())
        //fmNetworkManager.multipartRequest.deliverResponse(networkResponse)
        fmNetworkManager.multipartRequest.sequence = 1
        val cacheEntry: Cache.Entry = cacheTest!!.makeRandomCacheEntry(networkResponse)
        val successResponse = Response.success(networkResponse, cacheEntry)
        mDelivery!!.postResponse(fmNetworkManager.multipartRequest, successResponse)

        // Assert response
        //assertEquals(true, fmNetworkManager.multipartRequest.deliverResponseCalled)
        //assertEquals(false, fmNetworkManager.multipartRequest.deliverErrorCalled)
    }

    private fun mockMultiPartRequestUploadImage() {
        reqUploadImage = object : MockMultiPartRequest(
            Method.POST, "https://api.fantasmo.io/v1/image.localize",
            Response.Listener<NetworkResponse> { response ->
                reqRespUploadImage = response
                respUploadImage = String(response.data)
            },
            Response.ErrorListener {
                reqErrorUploadImage = it
            }) {

            override fun getByteData(): MutableMap<String, FileDataPart> {
                val params = HashMap<String, FileDataPart>()
                params["image"] = FileDataPart("image.jpg", getFileDataFromDrawable(
                    BitmapFactory.decodeResource(instrumentationContext.resources, R.drawable.image_on_street)) , "image/jpeg")
                return params
            }

            // Overriding getParams() to pass our parameters
            override fun getParams(): MutableMap<String, String> {
                return streetMockParameters()
            }

            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }
    }

    @Test
    fun testZoneInRadiusRequest() {
        // Make real request
        fmNetworkManager.zoneInRadiusRequest(
            "https://api.fantasmo.io/v1/parking.in.radius",
            getZoneInRadiusParams(radius),
            token
        ) {
            assertTrue(it)
        }
        // Assert fake and real requests
        assertNotNull(reqZoneIsInRadius)
        assertNotNull(fmNetworkManager.multipartRequest)

        // Assert fake and real request construction
        assertEquals(reqZoneIsInRadius?.url, fmNetworkManager.multipartRequest.url)
        assertEquals(reqZoneIsInRadius?.method, fmNetworkManager.multipartRequest.method)
        assertEquals(reqZoneIsInRadius?.headers, fmNetworkManager.multipartRequest.headers)
    }

    private fun mockMultiPartRequestZoneIsInRadius() {
        reqZoneIsInRadius = object : MockMultiPartRequest(
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
                return streetMockParameters()
            }

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