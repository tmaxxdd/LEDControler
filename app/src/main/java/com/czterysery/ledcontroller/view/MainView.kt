package com.czterysery.ledcontroller.view

/**
 * Created by tmax0 on 27.11.2018.
 */
interface MainView {

    fun setColorPickerColor(color: Int)

    fun setBrightnessValue(value: Int)

    fun setOnlyPhoneState(state: Boolean)

    fun setConnectionState(connected: Boolean)

    fun showMessage(text: String)

    fun showOnlyPhoneStateSwitch(state: Boolean)

}