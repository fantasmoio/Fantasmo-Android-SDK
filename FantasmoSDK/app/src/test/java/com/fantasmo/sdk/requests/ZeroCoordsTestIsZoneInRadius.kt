package com.fantasmo.sdk.requests

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class ZeroCoordsTestIsZoneInRadius {

    private lateinit var fmLocationManager: FMLocationManager
    private lateinit var context: Context
    private val token = "API_KEY"

    private val testScope = TestCoroutineScope()

    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token,fmLocationListener)
        fmLocationManager.coroutineScope = testScope
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testZeroCoordIsZoneInRadius(){
        fmLocationManager.isSimulation = false
        val latitude = 0.0
        val longitude = 0.0
        fmLocationManager.setLocation(latitude,longitude)

        val radius = 20
        var returnValue = false
        val start = System.currentTimeMillis()
        testScope.runBlockingTest {
            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
        }
        val stop = System.currentTimeMillis()
        val delta = stop-start

        assertTrue(delta >= 10000)
        assertEquals(false, returnValue)
        assertEquals(true, fmLocationManager.testTimeOut)
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