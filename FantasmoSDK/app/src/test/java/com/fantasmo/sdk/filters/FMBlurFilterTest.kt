package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.frameSequenceFilter.FMBlurFilterRule
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterFailure
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterResult
import com.google.ar.core.Frame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FMBlurFilterTest {

    private lateinit var fmBlurFilterRule: FMBlurFilterRule
    private lateinit var spyFMBlurFilterRule: FMBlurFilterRule
    private lateinit var instrumentationContext: Context

    @Before
    fun setUp() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        fmBlurFilterRule = FMBlurFilterRule(instrumentationContext)
        spyFMBlurFilterRule = spy(fmBlurFilterRule)
    }

    @Test
    fun testBlurFilterAccepts() {
        val frame = mock(Frame::class.java)

        doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            spyFMBlurFilterRule.check(frame)
        )
    }

    @Test
    fun testBlurFilterRejects() {
        val frame = mock(Frame::class.java)

        doReturn(250.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST),
            spyFMBlurFilterRule.check(frame)
        )
    }
}