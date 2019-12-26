package com.czterysery.ledcontroller.data.socket

import java.util.*
import java.util.logging.Handler
import kotlin.collections.HashMap

/**
 * Created by tmax0 on 28.11.2018.
 */
interface SocketManager {

    fun connect(address: String): Boolean

    fun disconnect(): Boolean

    fun writeMessage(message: String)

    fun readMessage(): String

    fun isBluetoothConnected(): Boolean

    fun isSocketConnected(): Boolean

    fun isBluetoothEnabled(): Boolean

    fun getDeviceAddress(name: String): String?

    fun getBluetoothDevices(): HashMap<String, String>

}