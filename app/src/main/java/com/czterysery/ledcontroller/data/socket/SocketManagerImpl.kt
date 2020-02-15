package com.czterysery.ledcontroller.data.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.czterysery.ledcontroller.Constants
import com.czterysery.ledcontroller.Messages.Companion.END_OF_LINE
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.model.Connected
import com.czterysery.ledcontroller.data.model.ConnectionState
import com.czterysery.ledcontroller.data.model.Disconnected
import com.czterysery.ledcontroller.data.model.Error
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun InputStream?.isAvailable() = when (this) {
    null -> false
    else -> this.available() > 0
}

// Defines in millis how often message will be read
const val READING_INTERVAL = 200L

class SocketManagerImpl : SocketManager {
    private val TAG = "SocketManager"
    private var btSocket: BluetoothSocket? = null
    private var messageReceiverDisposable: Disposable? = null

    override var connectionState: BehaviorSubject<ConnectionState> =
        BehaviorSubject.createDefault(Disconnected)

    override val messagePublisher: PublishSubject<String> =
        PublishSubject.create()

    override fun connect(address: String, btAdapter: BluetoothAdapter): Completable =
        Completable.fromCallable {
            runConnectionThread(address, btAdapter)
        }.doOnComplete {
            observeSerialPort()
        }.timeout(5, TimeUnit.SECONDS).doOnError { error ->
            if (error is TimeoutException)
                connectionState.onNext(Error(R.string.error_timeout))
        }

    override fun disconnect(): Completable =
        Completable.fromCallable {
            closeSources()
            connectionState.onNext(Disconnected)
        }.timeout(5, TimeUnit.SECONDS).doOnError {
            connectionState.onNext(Error(R.string.error_disconnect))
        }

    override fun writeMessage(message: String): Completable =
        Completable.fromCallable {
            btSocket?.outputStream?.write(message.toByteArray())
        }.doOnError { error -> Log.e(TAG, "Couldn't write message to the socket: $error") }

    private fun runConnectionThread(address: String, btAdapter: BluetoothAdapter) {
        ConnectThread(btAdapter, btAdapter.getRemoteDevice(address)).run()
    }

    private fun closeSources() {
        btSocket?.close()
        messageReceiverDisposable?.dispose()
    }

    private fun observeSerialPort() {
        messageReceiverDisposable?.dispose()
        if (connectionState.value is Connected)
            messageReceiverDisposable = streamObserver()
                .subscribeOn(Schedulers.io())
                .subscribe({ message ->
                    messagePublisher.onNext(message)
                    if (message.isNotBlank()) Log.d(TAG, "Received message: $message")
                }, { error ->
                    Log.e(TAG, "Cannot read a message: $error")
                })
    }

    private fun streamObserver(): Observable<String> =
        Observable.interval(READING_INTERVAL, TimeUnit.MILLISECONDS)
            .map { readStream(btSocket?.inputStream) }

    private fun readStream(stream: InputStream?): String {
        var output = ""
        when (stream) {
            null -> return output
            else -> while (stream.isAvailable()) {
                stream.let {
                    val char = it.read().toChar()
                    if (char != END_OF_LINE)
                        output += char
                    else
                        return output // On EOL symbol
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