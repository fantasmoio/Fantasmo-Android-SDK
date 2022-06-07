package com.example.fantasmo_android

import BaseRobot

import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class UIReleaseProtocolTests {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.CAMERA
    )

    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fantasmo_android", appContext.packageName)
    }

    @Test
    fun testAllToggleStates() {
        // test that we can toggle all toggles (switches) on and - wait for it - off!
        val alice = BaseRobot("Alice")

        alice.doOnView(LocalizeElements.debugStatsToggle(), click())
        alice.doOnView(LocalizeElements.qrCodeToggle(), click())
        alice.doOnView(LocalizeElements.simulationModToggle(), click())

        // need to check toggle state, not that it's displayed
        alice.assertOnView(LocalizeElements.debugStatsToggle(), matches(isDisplayed()))
        alice.assertOnView(LocalizeElements.qrCodeToggle(), matches(isDisplayed()))
        alice.assertOnView(LocalizeElements.simulationModToggle(), matches(isDisplayed()))

//        alice.attemptsTo(ToggleLocalizeSwitch(LocalizeSwitch.simulationMode))
//        alice.attemptsTo(ToggleLocalizeSwitch(LocalizeSwitch.debugStats))
//        alice.attemptsTo(ToggleLocalizeSwitch(LocalizeSwitch.qrCode))

//        alice.sees(Localize.ToggleState(of: LocalizeSwitch.qrCode, is: SwitchState.On))
//        alice.sees(Localize.ToggleState(of: LocalizeSwitch.debugStats, is: SwitchState.On))
//        alice.sees(Localize.ToggleState(of: LocalizeSwitch.simulationMode, is: SwitchState.On))


    }
}