package com.czterysery.ledcontroller.presenter

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.util.Log
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.Messages
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.*
import com.czterysery.ledcontroller.data.socket.SocketManager
import com.czterysery.ledcontroller.view.MainView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

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

    // TODO Remove
    private var colorChangeCounter = 3
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
        socketManager.disconnect()
    }

    // TODO Refactor
    override fun setColor(color: Int) {
        if (colorChangeCounter == 3) { //This blocks against multiple invocations with the same color
            val hexColor = String.format("#%06X", (0xFFFFFF and color))
            socketManager.writeMessage(Messages.SET_COLOR + hexColor + "\r\n")
            //colorChangeCounter = 0
        }
    }

    override fun setBrightness(value: Int) {
        socketManager.writeMessage(Messages.SET_BRIGHTNESS + value + "\r\n")
    }

    // TODO Refactor
    override fun setAnimation(anim: String) {
        for (i in 0..5) {
            Handler().postDelayed({
                socketManager.writeMessage(Messages.SET_ANIMATION + anim.toUpperCase() + "\r\n")
                //Log.d(TAG, "From reading message = ${socketManager.readMessage()}")
            }, 100)
        }
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

    private fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
            is Connected -> tryToGetConfiguration(state.device)
            Disconnected -> view?.showDisconnected()
            is Error -> view?.showError(state.message)
        }
        view?.showLoading(shouldShow = false)
    }

    private fun onBtStateChanged(state: BluetoothState) {
        when (state) {
            Enabled -> view?.showBtEnabled()
            Disabled -> view?.showBtDisabled()
            NotSupported -> view?.showBtNotSupported()
            None -> // When app starts, BT is in previous state. Check state manually.
                if (isBtEnabled()) view?.showBtEnabled() else view?.showBtDisabled()
        }
    }

    private fun sendConnectionMessage(connected: Boolean) {
        if (connected)
            socketManager.writeMessage(Messages.CONNECTED + "\r\n")
        else
            socketManager.writeMessage(Messages.DISCONNECTED + "\r\n")
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
                socketManager.connectionState.onNext(Error("Bluetooth is not available"))

            btController.getDeviceAddress(deviceName) == null ->
                socketManager.connectionState.onNext(Error("Cannot find the selected device"))

            else ->
                Completable.fromAction {
                    socketManager.connect(
                            btController.getDeviceAddress(deviceName) as String,
                            btController.adapter as BluetoothAdapter)
                }.subscribeOn(Schedulers.io())
                        .subscribe(
                                { sendConnectionMessage(true) },
                                { error ->
                                    Log.e(TAG, "Couldn't connect to device: $error")
                                    view?.showLoading(shouldShow = false)
                                }
                        )
        }
    }

    private fun tryToGetConfiguration(device: String) {
        // TODO `showLoading`
        // TODO Here create get configuartion with 5sec or less timeout
        // TODO `hideLoading`
        view?.showConnected(device)
    }

    private fun registerListeners() {
        subscribeBluetoothStateListener(bluetoothStateListener)
        subscribeConnectionStateListener(connectionStateListener)
    }

    private fun disposeAll() {
        btStateDisposable?.dispose()
        connectionStateDisposable?.dispose()
    }
}