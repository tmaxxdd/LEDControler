package com.czterysery.ledcontroller.presenter

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.util.Log
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.Messages
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.*
import com.czterysery.ledcontroller.data.socket.SocketManager
import com.czterysery.ledcontroller.view.MainView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

fun doNothing(): () -> Unit = {}

class MainPresenterImpl(
        private val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver,
        private val btController: BluetoothController,
        private val socketManager: SocketManager
) : MainPresenter {
    private val TAG = "MainPresenter"

    private val bluetoothStateListener: (state: BluetoothState) -> Unit =
            { state: BluetoothState ->
                onBtStateChanged(state)
            }

    private val connectionStateListener: (state: ConnectionState) -> Unit =
            { state ->
                onConnectionStateChanged(state)
            }

    private var btStateDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null
    private var messagePublisherDisposable: Disposable? = null
    private var messageWriterDisposable: Disposable? = null

    private var view: MainView? = null

    override fun onAttach(view: MainView) {
        this.view = view
        registerListeners()
    }

    override fun onDetach() {
        this.view = null
        disposeAll()
    }

    override fun connect() {
        if (isBtEnabled()) {
            showDevicesAndTryConnect()
        } else {
            view?.showBtDisabled()
        }
    }

    override fun disconnect() {
        sendConnectionMessage(connected = false)
        socketManager.disconnect().subscribe(
                doNothing(), { error ->
            Log.e(TAG, "Couldn't close the socket: $error")
        })
    }

    override fun setColor(color: Int) {
        val hexColor = String.format("#%06X", (0xFFFFFF and color))
        writeMessage(Messages.SET_COLOR + hexColor + "\r\n")
    }

    override fun setBrightness(value: Int) {
        writeMessage(Messages.SET_BRIGHTNESS + value + "\r\n")
    }

    override fun setAnimation(anim: String) {
        writeMessage(Messages.SET_ANIMATION + anim.toUpperCase() + "\r\n")
    }

    /* Get the current params from an ESP32 */
// TODO Implement this and rename
    private fun loadCurrentParams() {
//        getColor()
//        getBrightness()
    }

    override fun isConnected() = socketManager.connectionState.value is Connected

    override fun isBtEnabled(): Boolean = btController.isEnabled

    private fun subscribeConnectionStateListener(listener: (state: ConnectionState) -> Unit) {
        connectionStateDisposable?.dispose()
        connectionStateDisposable = socketManager.connectionState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> listener(state) },
                        { error -> Log.e(TAG, "Error during observing connection state: $error") }
                )
    }

    private fun subscribeBluetoothStateListener(listener: (state: BluetoothState) -> Unit) {
        btStateDisposable?.dispose()
        btStateDisposable = bluetoothStateBroadcastReceiver.btState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> checkIfBtSupportedAndReturnState(listener, state) },
                        { error -> Log.e(TAG, "Error during observing BT state: $error") }
                )
    }

    private fun subscribeMessagePublisher() {
        messagePublisherDisposable?.dispose()
        messagePublisherDisposable = socketManager.messagePublisher
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { message -> parseMessage(message) },
                        { view?.showError(R.string.error_receiving_message) }
                )
    }

    private fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
            is Connected -> {
                view?.showConnected(state.device)
                subscribeMessagePublisher()
                tryToGetConfiguration()
            }
            Disconnected -> {
                view?.showDisconnected()
            }
            is Error -> view?.showError(state.messageId)
        }
        view?.showLoading(shouldShow = false)
    }

    private fun onBtStateChanged(state: BluetoothState) {
        when (state) {
            Enabled -> view?.showBtEnabled()
            Disabled -> {
                socketManager.connectionState.onNext(Disconnected)
                view?.showBtDisabled()
            }
            NotSupported -> view?.showBtNotSupported()
            None -> // When app starts, BT is in previous state. Check state manually.
                if (isBtEnabled()) view?.showBtEnabled() else view?.showBtDisabled()
        }
    }

    private fun writeMessage(message: String) {
        messageWriterDisposable?.dispose()
        messageWriterDisposable = socketManager.writeMessage(message)
                .subscribe()
    }

    private fun sendConnectionMessage(connected: Boolean) {
        if (connected)
            writeMessage(Messages.CONNECTED + "\r\n")
        else
            writeMessage(Messages.DISCONNECTED + "\r\n")
    }

    private fun checkIfBtSupportedAndReturnState(listener: (state: BluetoothState) -> Unit, state: BluetoothState) {
        if (btController.isSupported.not())
            listener(NotSupported)
        else
            listener(state)
    }

    private fun showDevicesAndTryConnect() {
        view?.let {
            val devices = btController.getDevices().keys.toTypedArray()
            if (devices.isNotEmpty()) {
                it.showLoading()
                it.showDevicesList(devices) { dialog, deviceName ->
                    // On selected device
                    dialog.dismiss()
                    tryToConnectWithDevice(deviceName)
                }
            } else {
                it.showPairWithDevice()
            }
        }
    }

    private fun tryToConnectWithDevice(deviceName: String) {
        when {
            btController.adapter == null ->
                socketManager.connectionState.onNext(Error(R.string.error_bt_not_available))

            btController.getDeviceAddress(deviceName) == null ->
                socketManager.connectionState.onNext(Error(R.string.error_cannot_find_device))

            else ->
                socketManager.connect(
                        btController.getDeviceAddress(deviceName) as String,
                        btController.adapter as BluetoothAdapter
                ).subscribeOn(Schedulers.io())
                        .subscribe({
                            sendConnectionMessage(connected = true)
                        }, { error ->
                            view?.showLoading(shouldShow = false)
                            Log.e(TAG, "Couldn't connect to device: $error")
                        })
        }
    }

    private fun tryToGetConfiguration() {
        // TODO `showLoading`
        // TODO Here create get configuartion with 5sec or less timeout
        // TODO `hideLoading`
    }

    private fun parseMessage(message: String) {
        Log.d(TAG, "Message = $message")
    }

    private fun registerListeners() {
        subscribeBluetoothStateListener(bluetoothStateListener)
        subscribeConnectionStateListener(connectionStateListener)
    }

    private fun disposeAll() {
        messageWriterDisposable?.dispose()
        messagePublisherDisposable?.dispose()
        btStateDisposable?.dispose()
        connectionStateDisposable?.dispose()
    }
}