package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

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
        fmBlurFilter = FMBlurFilter(instrumentationContext)
        spyFMBlurFilter = spy(fmBlurFilter)
    }

    @Test
    fun testBlurFilterAccepts() {
        val frame = mock(Frame::class.java)
        val byteArrayOutputStream = mock(ByteArrayOutputStream::class.java)

        testScope.launch(Dispatchers.Default){
            doReturn(300.0).`when`(spyFMBlurFilter).calculateVariance(byteArrayOutputStream)

            Assert.assertEquals(
                null,
                spyFMBlurFilter.accepts(frame).getRejectedReason()
            )
        }
    }

    @Test
    fun testBlurFilterRejects() {
        val frame = mock(Frame::class.java)
        val byteArrayOutputStream = mock(ByteArrayOutputStream::class.java)

        testScope.launch(Dispatchers.Default) { // launches coroutine in cpu thread
            doReturn(250.0).`when`(spyFMBlurFilter).calculateVariance(byteArrayOutputStream)
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