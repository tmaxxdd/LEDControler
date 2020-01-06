package com.czterysery.ledcontroller.presenter

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.util.Log
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.Messages
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.BluetoothState
import com.czterysery.ledcontroller.data.model.Connected
import com.czterysery.ledcontroller.data.model.ConnectionState
import com.czterysery.ledcontroller.data.model.NotSupported
import com.czterysery.ledcontroller.data.socket.SocketManager
import com.czterysery.ledcontroller.view.MainView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainPresenterImpl(
        private val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver,
        private val bluetoothController: BluetoothController,
        private val socketManager: SocketManager
) : MainPresenter {
    private val TAG = "MainPresenter"

    private var btStateDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null

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
        btStateDisposable = bluetoothStateBroadcastReceiver
                .btState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> checkIfBtSupportedAndReturnState(listener, state) },
                        { error -> Log.e(TAG, "Error during observing BT state: $error") }
                )
    }

    override fun setConnectionStateListener(listener: (state: ConnectionState) -> Unit) {
        connectionStateDisposable?.dispose()
        connectionStateDisposable = socketManager
                .connectionState
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { state -> listener(state) },
                        { error -> Log.e(TAG, "Error during observing connection state: $error") }
                )
    }

    override fun connect(context: Context) {
        if (isFullyConnected()) {
            view?.showMessage("Already connected.")
        } else {
            //Not connected, try to connect
            selectDevice(context)
        }
    }

    override fun disconnect() {
        if (socketManager.isBluetoothEnabled()) {
            if (socketManager.isSocketConnected()) {
                socketManager.writeMessage(Messages.DISCONNECTED + "\r\n")
                if (socketManager.disconnect()) {
                    view?.updateConnectionState(false)
                    view?.showMessage("Disconnected from device.")
                } else {
                    view?.showMessage("Error during disconnecting from socket.")
                }
            }
        }
    }

    override fun setColor(color: Int) {
        if (colorChangeCounter == 3) { //This blocks against multiple invocations with the same color
            isFullyConnected().let {
                val hexColor = String.format("#%06X", (0xFFFFFF and color))
                socketManager.writeMessage(Messages.SET_COLOR + hexColor + "\r\n")
                //colorChangeCounter = 0
            }
        } else {
            //colorChangeCounter++
        }
    }

    override fun setBrightness(value: Int) {
        isFullyConnected().let {
            socketManager.writeMessage(Messages.SET_BRIGHTNESS + value + "\r\n")
        }
    }

    override fun setOnlyPhoneMode(state: Boolean) {
        isFullyConnected().let {
            socketManager.writeMessage(Messages.SET_ONLY_PHONE_MODE + state + "\r\n")
        }
    }

    override fun setAnimation(anim: String) {
        isFullyConnected().let {

            for (i in 0..5) {
                Handler().postDelayed({
                    socketManager.writeMessage(Messages.SET_ANIMATION + anim.toUpperCase() + "\r\n")
                    //Log.d(TAG, "From reading message = ${socketManager.readMessage()}")
                }, 100)
            }
        }
    }

    /* Get the current params from an ESP32 */
    override fun loadCurrentParams() {
        getColor()
        getBrightness()
    }

    override fun getColor() {
        //view?.updateCurrentColor(color)
    }

    override fun getBrightness() {
        //view?.setBrightnessValue(value)
    }

    override fun isConnected() =
            socketManager.connectionState.value is Connected

    override fun isBtEnabled(): Boolean = bluetoothController.isEnabled

    private fun checkIfBtSupportedAndReturnState(listener: (state: BluetoothState) -> Unit, state: BluetoothState) {
        if (bluetoothController.isSupported.not())
            listener(NotSupported)
        else
            listener(state)
    }

    private fun isFullyConnected(): Boolean {
        if (socketManager.isBluetoothEnabled()) {
            if (socketManager.isSocketConnected()) {
                return true
            }
        } else {
            view?.showMessage("Please turn on Bluetooth!")
        }

        return false
    }

    private fun tryConnect(address: String) {
        if (socketManager.connect(address)) {
            onConnected()
        } else {
            view?.showMessage("Can't connect.")
        }
    }

    private fun onConnected() {
        view?.let {
            it.showMessage("Connected!")
            it.updateConnectionState(true)
        }
        socketManager.writeMessage(Messages.CONNECTED + "\r\n")
    }

    private fun disposeAll() {
        btStateDisposable?.dispose()
        connectionStateDisposable?.dispose()
    }

    // TODO Export to the `DialogManger`
    private fun selectDevice(context: Context) {
        val btDialog = AlertDialog.Builder(context)
        val devices = socketManager.getBluetoothDevices()
        val titles = devices.keys.toTypedArray() //Get names
        btDialog.setTitle("Available devices")
        btDialog.setItems(titles) { _, which ->
            //On Click
            //Return bluetooth's address for selected device
            val selectedDevice = devices[titles[which]]!!
            tryConnect(selectedDevice)
        }
        btDialog.show()
    }
}