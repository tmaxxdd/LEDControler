package com.czterysery.ledcontroller.presenter

import android.content.Context
import com.czterysery.ledcontroller.data.model.BluetoothState
import com.czterysery.ledcontroller.data.model.ConnectionState

interface MainPresenter: BasePresenter {

    fun setBluetoothStateListener(listener: (state: BluetoothState) -> Unit)

    fun setConnectionStateListener(listener: (state: ConnectionState) -> Unit)

    fun connect(context: Context)

    fun disconnect()

    fun setColor(color: Int)

    fun setBrightness(value: Int)

    fun setAnimation(anim: String)

    fun sendConnectionMessage(connected: Boolean)

    // TODO Replace it with getConfiguration: Configuration()
    fun loadCurrentParams()

    fun isConnected(): Boolean

    fun isBtEnabled(): Boolean

}