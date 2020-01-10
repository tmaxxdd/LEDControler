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
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainPresenterImpl(
        private val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver,
        private val btController: BluetoothController,
        private val socketManager: SocketManager
) : MainPresenter {
    private val TAG = "MainPresenter"

    private var btStateDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null

    // TODO Remove
    private var colorChangeCounter = 3
    private var view: MainView? = null

    override fun onAttach(view: MainView) {
        this.view = view
    }

    override fun onDetach() {
        this.view = null
        disposeAll()
    }

    override fun setBluetoothStateListener(listener: (state: BluetoothState) -> Unit) {
        btStateDisposable?.dispose()
        btStateDisposable = bluetoothStateBroadcastReceiver.btState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> checkIfBtSupportedAndReturnState(listener, state) },
                        { error -> Log.e(TAG, "Error during observing BT state: $error") }
                )
    }

    override fun setConnectionStateListener(listener: (state: ConnectionState) -> Unit) {
        connectionStateDisposable?.dispose()
        connectionStateDisposable = socketManager.connectionState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> listener(state) },
                        { error -> Log.e(TAG, "Error during observing connection state: $error") }
                )
    }

    override fun connect() {
        view?.let {
            val devices = btController.getDevices().keys.toTypedArray()
            if (devices.isNotEmpty()) {
                it.showDevicesList(devices) { deviceName ->
                    // On selected device
                    it.showLoading()
                    tryToConnectWithDevice(deviceName)
                }
            } else {
                it.showPairWithDevice()
            }
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

    override fun sendConnectionMessage(connected: Boolean) {
        if (connected)
            socketManager.writeMessage(Messages.CONNECTED + "\r\n")
        else
            socketManager.writeMessage(Messages.DISCONNECTED + "\r\n")
    }

    /* Get the current params from an ESP32 */
    // TODO Implement this
    override fun loadCurrentParams() {
//        getColor()
//        getBrightness()
    }

    override fun isConnected() =
            socketManager.connectionState.value is Connected

    override fun isBtEnabled(): Boolean = btController.isEnabled

    private fun checkIfBtSupportedAndReturnState(listener: (state: BluetoothState) -> Unit, state: BluetoothState) {
        if (btController.isSupported.not())
            listener(NotSupported)
        else
            listener(state)
    }

    private fun tryToConnectWithDevice(deviceName: String) {
        when {
            btController.adapter == null ->
                socketManager.connectionState.onNext(Error("Bluetooth is not available"))

            btController.getDeviceAddress(deviceName) == null ->
                socketManager.connectionState.onNext(Error("Cannot find the selected device"))

            else ->
                socketManager.connect(
                        btController.getDeviceAddress(deviceName) as String,
                        btController.adapter as BluetoothAdapter
                )
        }
    }

    private fun disposeAll() {
        btStateDisposable?.dispose()
        connectionStateDisposable?.dispose()
    }
}