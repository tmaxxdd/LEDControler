package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import com.czterysery.ledcontroller.data.model.ConnectionState
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

interface SocketManager {

    val connectionState: BehaviorSubject<ConnectionState>

    val messagePublisher: PublishSubject<String>

    fun connect(address: String, btAdapter: BluetoothAdapter): Completable

    fun disconnect(): Completable

    fun writeMessage(message: String): Completable

}