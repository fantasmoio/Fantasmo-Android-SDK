package com.fantasmo.sdk.requests

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.ResponseDelivery
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMConfiguration
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.network.FMNetworkManager
import com.fantasmo.sdk.utils.ImmediateResponseDelivery
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class IsZoneInRadiusTest {

    private lateinit var fmLocationManager: FMLocationManager

    private lateinit var spyFMLocationManager: FMLocationManager
    private lateinit var spyFMNetworkManager: FMNetworkManager
    private lateinit var spyFMApi: FMApi
    private lateinit var context: Context
    private val token = "API_KEY"

    private val testScope = TestCoroutineScope()

    private var mDelivery: ResponseDelivery? = null

    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        mDelivery = ImmediateResponseDelivery()
        fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token,fmLocationListener)
        fmLocationManager.coroutineScope = testScope

        spyFMLocationManager = Mockito.spy(fmLocationManager)

        spyFMNetworkManager = Mockito.spy(spyFMLocationManager.fmNetworkManager)
        spyFMApi = Mockito.spy(spyFMLocationManager.fmApi)
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testSetLocation(){
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)
        assertNotNull(fmLocationManager.currentLocation)
    }

    @Test
    fun testIsZoneInRadius() {
        fmLocationManager.isConnected = false
        fmLocationManager.isSimulation = true

        var radius = 10
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
            assertEquals(true, it)
        }

        radius = 100
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
            assertEquals(false, it)
        }
    }

    @Test
    fun testIsZoneInRadiusNoSimulation(){
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val radius = 20
        var returnValue = false
        testScope.runBlockingTest {
            spyFMLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
            assertEquals(false, returnValue)
        }
        verify(spyFMLocationManager, Mockito.times(2)).fmApi
        verify(spyFMLocationManager, Mockito.times(1)).fmNetworkManager
    }

    @Test
    fun testIsZoneInRadiusSimulation(){
        fmLocationManager.isSimulation = true

        val radius = 20
        var returnValue = false
        testScope.runBlockingTest {
            spyFMLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
            assertEquals(false, returnValue)
        }
        verify(spyFMLocationManager, Mockito.times(2)).fmApi
        verify(spyFMLocationManager, Mockito.times(1)).fmNetworkManager
    }


    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
            }

            override fun locationManager(location: Location, zones: List<FMZone>?) {
            }
        }
}