package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.czterysery.ledcontroller.Constants
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.Connected
import com.czterysery.ledcontroller.data.model.ConnectionState
import com.czterysery.ledcontroller.data.model.Disconnected
import com.czterysery.ledcontroller.data.model.Error
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.functions.Supplier
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import org.reactivestreams.Publisher
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun InputStream?.isAvailable() = when (this) {
    null -> false
    else -> this.available() > 0
}

class SocketManagerImpl : SocketManager {
    private val TAG = "SocketManager"
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
    private var btSocket: BluetoothSocket? = null

    override var connectionState: BehaviorSubject<ConnectionState> =
            BehaviorSubject.createDefault(Disconnected)

    override val messagePublisher: PublishSubject<String> =
            PublishSubject.create()

    override fun connect(address: String, btAdapter: BluetoothAdapter): Completable =
            Completable.fromCallable {
                ConnectThread(
                        btAdapter,
                        btAdapter.getRemoteDevice(address)
                ).run()
            }.doOnComplete {
                observeSerialPort()
            }.timeout(5, TimeUnit.SECONDS).doOnError { error ->
                Log.e(TAG, "Couldn't connect to device: $error")
                if (error is TimeoutException)
                    connectionState.onNext(Error(R.string.error_timeout))
            }

    override fun disconnect(): Completable =
            Completable.fromCallable {
                btSocket?.close()
                connectionState.onNext(Disconnected)
            }.timeout(5, TimeUnit.SECONDS).doOnError { error ->
                Log.e(TAG, "Couldn't close the client socket: $error")
                connectionState.onNext(Error(R.string.error_disconnect))
            }

    override fun writeMessage(message: String): Completable =
            Completable.fromCallable {
                btSocket?.outputStream?.use {
                    it.write(message.toByteArray())
                }
            }.doOnError { error -> Log.e(TAG, "Couldn't write message to the socket: $error") }


//        var len = 0
//        if (btSocket != null) {
//            try {
//                len = btSocket!!.inputStream.available()
//                while (btSocket!!.inputStream.available() > 0) {
//                    btSocket!!.inputStream.read(mmBuffer)
//                    Log.d(TAG, "Message = ${String(mmBuffer, 0, len)}")
//                    Log.d(TAG, "Bytes to read = ${btSocket!!.inputStream.available()}")
//                }
//                return String(mmBuffer, 0, len)
//            } catch (e: IOException) {
//                Log.d(TAG, "Cannot read a message")
//            }
//        }
//        return ""
//    }

    private fun observeSerialPort() {

        // TODO Consider using kotlin's 'use' or Flowable with using()

        Flowable.using(
                Supplier<String> { readStream(btSocket?.inputStream) },
                Function<String, Publisher<String>> { d -> return@Function Publisher<String> { d } },
                Consumer<String> { d -> d is String }
        ).subscribe {  }
    }

    private fun readStream(stream: InputStream?): String {
        var output = ""
        when (stream) {
            null -> return output
            else ->
                // Collect the data from stream
                stream.use { stream ->
                    // `use` will always close the stream
                    while (stream.isAvailable()) {
                        stream.let { output += it.read().toChar() }
                    }
                }
        }

        return output
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
                    connectionState.onNext(Error(R.string.error_timeout))
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