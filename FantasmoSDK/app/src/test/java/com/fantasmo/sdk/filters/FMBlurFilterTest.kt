package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.config.RemoteConfigTest
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ignoring Tests due to Renderscript failure
 * Cannot replicate context to create Renderscript environment
 */
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
@Ignore
class FMBlurFilterTest {

    private lateinit var fmBlurFilter: FMBlurFilter
    private lateinit var spyFMBlurFilter: FMBlurFilter
    private lateinit var instrumentationContext: Context

    private val testScope = TestCoroutineScope()

    @Before
    fun setUp() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context

        fmBlurFilter = FMBlurFilter(
            RemoteConfigTest.remoteConfig.blurFilterVarianceThreshold,
            RemoteConfigTest.remoteConfig.blurFilterSuddenDropThreshold,
            RemoteConfigTest.remoteConfig.blurFilterAverageThroughputThreshold,
            instrumentationContext
        )
        val fieldRenderScriptContext = fmBlurFilter.javaClass.getDeclaredField("rs")
        fieldRenderScriptContext.isAccessible = true
        fieldRenderScriptContext.set(fmBlurFilter, null)

        val fieldColorIntrinsic = fmBlurFilter.javaClass.getDeclaredField("colorIntrinsic")
        fieldColorIntrinsic.isAccessible = true
        fieldColorIntrinsic.set(fmBlurFilter, null)

        val fieldConvolve = fmBlurFilter.javaClass.getDeclaredField("convolve")
        fieldConvolve.isAccessible = true
        fieldConvolve.set(fmBlurFilter, null)

        spyFMBlurFilter = spy(fmBlurFilter)
    }

    @Test
    fun testBlurFilterAccepts() {
        val frame = mock(Frame::class.java)
        val byteArray = mock(ByteArray::class.java)

        testScope.launch(Dispatchers.Default) {
            doReturn(300.0).`when`(spyFMBlurFilter).calculateVariance(byteArray)

            Assert.assertEquals(
                null,
                spyFMBlurFilter.accepts(frame).getRejectedReason()
            )
        }
    }

    @Test
    fun testBlurFilterRejects() {
        val frame = mock(Frame::class.java)
        val byteArray = mock(ByteArray::class.java)

        testScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            doReturn(250.0).`when`(spyFMBlurFilter).calculateVariance(byteArray)
            Assert.assertEquals(
                FMFilterRejectionReason.IMAGETOOBLURRY,
                spyFMBlurFilter.accepts(frame).getRejectedReason()
            )
        }
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }
}