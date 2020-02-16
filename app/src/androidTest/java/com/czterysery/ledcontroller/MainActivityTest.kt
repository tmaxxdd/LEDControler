package com.czterysery.ledcontroller

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.czterysery.ledcontroller.view.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    // TODO These test pass always
    @Test
    fun color_picker_should_be_semi_transparent_on_start() {
        onView(withId(R.id.colorPicker))
            .perform(click())
            .check { view, _ -> assert(view.alpha == 0.5f) }
    }

    @Test
    fun illumination_dropdown_should_be_disabled_on_start() {
        onView(withId(R.id.illuminationDropdown))
            .perform(click())
            .check { view, _ -> assert(view.isEnabled) } //TODO Passed and should fail
    }

    @Test
    fun brightness_slider_should_be_disabled_on_start() {

    }

}