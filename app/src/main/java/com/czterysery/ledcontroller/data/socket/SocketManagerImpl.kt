package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.czterysery.ledcontroller.Constants
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import java.io.IOException

/**
 * Created by tmax0 on 27.11.2018.
 */
class SocketManagerImpl : SocketManager {
    private val TAG = "SocketManager"
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    companion object {
        private var connectionSuccess = false
        private var isBtConnected = false
        private var btSocket: BluetoothSocket? = null
        private var myBluetooth: BluetoothAdapter? = null
    }

    private val bluetoothController = BluetoothController()

    override fun connect(address: String): Boolean {
        myBluetooth = bluetoothController.adapter
        val device = myBluetooth?.getRemoteDevice(address)
        ConnectThread(myBluetooth!!, device!!).run()
        return connectionSuccess
    }

    override fun disconnect(): Boolean {
        return if (btSocket != null) { //If the btSocket is busy
            try {
                btSocket!!.close() //close connection
                isBtConnected = false
                connectionSuccess = false
                true
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
                false
            }
        } else {
            false
        }
    }

    override fun writeMessage(message: String) {
        if (btSocket != null) {
            try {
                btSocket!!.outputStream.write(message.toByteArray())
            } catch (e: IOException) {
                Log.d(TAG, "Couldn't write message to the socket")
            }

        }
    }

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

    override fun isBluetoothConnected(): Boolean = isBtConnected

    override fun isSocketConnected(): Boolean = connectionSuccess

    override fun isBluetoothEnabled(): Boolean = bluetoothController.isEnabled

    override fun getDeviceAddress(name: String): String? = bluetoothController.getDeviceAddress(name)

    override fun getBluetoothDevices(): HashMap<String, String> {
        return bluetoothController.getDevices()
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
                    Log.d(TAG, "Timeout! Cannot connect to a device")
                    cancel()
                }
                if (socket.isConnected) {
                    Log.d(TAG,"Connected to the ${device.name}")
                    connectionSuccess = true
                } else {
                    Log.d(TAG,"Connection Failed. Is it a SPP Bluetooth? Try again.")
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
                isBtConnected = false
                connectionSuccess = false
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