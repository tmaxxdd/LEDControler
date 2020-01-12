package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.czterysery.ledcontroller.Constants
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.Connected
import com.czterysery.ledcontroller.data.model.ConnectionState
import com.czterysery.ledcontroller.data.model.Disconnected
import com.czterysery.ledcontroller.data.model.Error
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SocketManagerImpl : SocketManager {
    private val TAG = "SocketManager"
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    companion object {
        private var btSocket: BluetoothSocket? = null
    }

    override var connectionState: BehaviorSubject<ConnectionState> =
            BehaviorSubject.createDefault(Disconnected)

    override fun connect(address: String, btAdapter: BluetoothAdapter): Completable =
            Completable.fromCallable {
                val device = btAdapter.getRemoteDevice(address)
                ConnectThread(btAdapter, device).run()
            }.timeout(5, TimeUnit.SECONDS).doOnError { error ->
                if (error is TimeoutException)
                    connectionState.onNext(Error("Cannot connect. Timeout!"))
            }

    override fun disconnect() {
        btSocket?.let {
            try {
                btSocket!!.close() //close connection
                connectionState.onNext(Disconnected)
            } catch (e: IOException) {
                Log.e(TAG, "Couldn't close the client socket", e)
                connectionState.onNext(Error("Can't disconnect!"))
            }
        }
    }

    // TODO Refactor
    override fun writeMessage(message: String) {
        btSocket?.let {
            try {
                btSocket!!.outputStream.write(message.toByteArray())
            } catch (e: IOException) {
                Log.d(TAG, "Couldn't write message to the socket")
            }
        }
    }

    // TODO Refactor
    override fun readMessage(): String {
        var len = 0
        if (btSocket != null) {
            try {
                len = btSocket!!.inputStream.available()
                while (btSocket!!.inputStream.available() > 0) {
                    btSocket!!.inputStream.read(mmBuffer)
                    Log.d(TAG, "Message = ${String(mmBuffer, 0, len)}")
                    Log.d(TAG, "Bytes to read = ${btSocket!!.inputStream.available()}")
                }
                return String(mmBuffer, 0, len)
            } catch (e: IOException) {
                Log.d(TAG, "Cannot read a message")
            }
        }
        return ""
    }

    private inner class ConnectThread(private val adapter: BluetoothAdapter, private val device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(Constants.UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            adapter.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.

                try {
                    socket.connect()
                } catch (e: Exception) {
                    connectionState.onNext(Error("Timeout! Cannot connect to a device"))
                    cancel()
                }

                if (socket.isConnected) {
                    connectionState.onNext(Connected(device.name))
                } else {
                    cancel()
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }

        fun manageMyConnectedSocket(socket: BluetoothSocket) {
            //Give the SocketManager access to the active socket
            btSocket = socket
        }
    }

}