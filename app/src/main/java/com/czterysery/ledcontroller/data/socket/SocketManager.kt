package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import com.czterysery.ledcontroller.data.model.ConnectionState
import io.reactivex.rxjava3.subjects.BehaviorSubject

interface SocketManager {

    val connectionState: BehaviorSubject<ConnectionState>

    fun connect(address: String, btAdapter: BluetoothAdapter)

    fun disconnect()

    fun writeMessage(message: String)

    fun readMessage(): String
}