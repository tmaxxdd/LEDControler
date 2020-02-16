package com.czterysery.ledcontroller

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.czterysery.ledcontroller.view.MainActivity
import com.czterysery.ledcontroller.view.MainView
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun color_picker_should_be_semi_transparent_on_start() {
        onView(withId(R.id.colorPicker))
            .check(matches(withAlpha(0.5f)))
    }

    @Test
    fun illumination_dropdown_should_be_disabled_on_start() {
        onView(withId(R.id.illuminationDropdown))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun brightness_slider_should_be_disabled_on_start() {
        onView(withId(R.id.brightnessSlider))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun button_should_have_connect_text_on_start() {
        onView(withId(R.id.connectAction))
            .check(matches(withText(activityRule.activity.getString(R.string.connect))))
    }

    @Test
    fun when_connected_should_show_snackbar_with_connected_message() {
        activityRule.runOnUiThread {
            (activityRule.activity as MainView).showConnected(DEVICE)
        }
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(
                withText(activityRule.activity.getString(R.string.connected_with, DEVICE)))
            )
    }

    @Test
    fun when_connected_color_picker_should_not_be_transparent() {
        activityRule.runOnUiThread {
            (activityRule.activity as MainView).showConnected(DEVICE)
        }
        onView(withId(R.id.colorPicker))
            .check(matches(withAlpha(1f)))
    }

    @Test
    fun when_connected_dropdown_should_be_enabled() {
        activityRule.runOnUiThread {
            (activityRule.activity as MainView).showConnected(DEVICE)
        }

        onView(withId(R.id.illuminationDropdown))
            .check(matches(isEnabled()))
    }

    @Test
    fun when_connected_brightness_slider_should_be_enabled() {
        activityRule.runOnUiThread {
            (activityRule.activity as MainView).showConnected(DEVICE)
        }

        onView(withId(R.id.brightnessSlider))
            .check(matches(isEnabled()))
    }

    @Test
    fun when_connected_button_should_have_disconnect_text() {
        activityRule.runOnUiThread {
            (activityRule.activity as MainView).showConnected(DEVICE)
        }

        onView(withId(R.id.connectAction))
            .check(matches(withText(R.string.disconnect)))
    }

    @Test
    fun when_disconnected_should_show_snackbar_with_disconnected_message() {
        activityRule.runOnUiThread {
            with(activityRule.activity as MainView) {
                showConnected(DEVICE)
                showDisconnected()
            }
        }
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(
                withText(activityRule.activity.getString(R.string.disconnected)))
            )
    }

    companion object {
        const val DEVICE = "BT device"
    }
}