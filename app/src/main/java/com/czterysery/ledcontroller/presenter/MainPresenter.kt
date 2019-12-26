package com.czterysery.ledcontroller.presenter

import android.content.Context

/**
 * Created by tmax0 on 27.11.2018.
 */
interface MainPresenter: BasePresenter {

    fun connectToBluetooth(context: Context)

    fun disconnect()

    fun setColor(color: Int)

    fun setBrightness(value: Int)

    fun setOnlyPhoneMode(state: Boolean)

    fun setAnimation(anim: String)

    fun loadCurrentParams()

    fun getColor()

    fun getBrightness()

    fun isConnected()

    fun isOnlyPhoneMode()

}