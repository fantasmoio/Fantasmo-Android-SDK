package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.YuvImage
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.config.RemoteConfigTest
import com.fantasmo.sdk.models.FMFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
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

        val fieldHistogram = fmBlurFilter.javaClass.getDeclaredField("histogram")
        fieldHistogram.isAccessible = true
        fieldHistogram.set(fmBlurFilter, null)

        val fieldConvolve = fmBlurFilter.javaClass.getDeclaredField("convolve")
        fieldConvolve.isAccessible = true
        fieldConvolve.set(fmBlurFilter, null)

        val fieldResize = fmBlurFilter.javaClass.getDeclaredField("resize")
        fieldResize.isAccessible = true
        fieldResize.set(fmBlurFilter, null)

        spyFMBlurFilter = spy(fmBlurFilter)
    }

    @Test
    fun testBlurFilterAccepts() {
        val frame = mock(FMFrame::class.java)
        val yuvImage = mock(YuvImage::class.java)

        testScope.launch(Dispatchers.Default) {
            doReturn(300.0).`when`(spyFMBlurFilter).calculateVariance(yuvImage)

            Assert.assertEquals(
                null,
                spyFMBlurFilter.accepts(frame).getRejectedReason()
            )
        }
    }

    @Test
    fun testBlurFilterRejects() {
        val frame = mock(FMFrame::class.java)
        val yuvImage = mock(YuvImage::class.java)

        testScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            doReturn(250.0).`when`(spyFMBlurFilter).calculateVariance(yuvImage)
            Assert.assertEquals(
                FMFrameFilterRejectionReason.IMAGE_TOO_BLURRY,
                spyFMBlurFilter.accepts(frame).getRejectedReason()
            )
        }
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }
}