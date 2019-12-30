package com.czterysery.ledcontroller.presenter

import android.content.BroadcastReceiver
import android.content.Context
import com.czterysery.ledcontroller.data.model.BluetoothState


interface MainPresenter: BasePresenter {

    fun setBluetoothStateListener(listener: (state: BluetoothState) -> Unit): BroadcastReceiver

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