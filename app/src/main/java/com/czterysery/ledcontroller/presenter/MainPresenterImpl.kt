package com.czterysery.ledcontroller.presenter

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.support.v7.view.ContextThemeWrapper
import android.util.Log
import com.czterysery.ledcontroller.Constants
import com.czterysery.ledcontroller.Messages
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.socket.SocketManager
import com.czterysery.ledcontroller.view.MainView
import kotlinx.coroutines.delay

class MainPresenterImpl(private val socketManager: SocketManager) : MainPresenter {
    private val TAG = "MainPresenterImpl"
    private var colorChangeCounter = 3
    private var view: MainView? = null

    /* Control the main view state */

    /* App is shown */
    override fun onAttach(view: MainView) {
        this.view = view
    }

    /* App has disappeared */
    override fun onDetach() {
        this.view = null
    }

    /* Control the LED settings */

    override fun connectToBluetooth(context: Context) {

            if (isFullyConnected()){
                //Is currently connected with the socket
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
                    view?.setConnectionState(false)
                    view?.showMessage("Disconnected from device.")
                } else {
                    view?.showMessage("Error during disconnecting from socket.")
                }
            }
        }
    }

    override fun setColor(color: Int) {
        if(colorChangeCounter == 3){ //This blocks against multiple invocations with the same color
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
                Handler().postDelayed( {
                    socketManager.writeMessage(Messages.SET_ANIMATION + anim.toUpperCase() + "\r\n")
                    //Log.d(TAG, "From reading message = ${socketManager.readMessage()}")
                }, 100)

            }
        }
    }

    /* Get the current params from an ESP32 */
    override fun loadCurrentParams() {
        isConnected()
        isOnlyPhoneMode()
        getColor()
        getBrightness()
    }

    override fun getColor() {
        //view?.setColorPickerColor(color)
    }

    override fun getBrightness() {
        //view?.setBrightnessValue(value)
    }

    override fun isConnected() {
        //view?.setConnectionState()
    }

    override fun isOnlyPhoneMode() {

    }

    private fun isFullyConnected(): Boolean {
        if (socketManager.isBluetoothEnabled()){
            if (socketManager.isSocketConnected()){
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
            it.setConnectionState(true)
        }
        socketManager.writeMessage(Messages.CONNECTED + "\r\n")
    }

    private fun selectDevice(context: Context) {
        val btDialog = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        val devices = socketManager.getBluetoothDevices()
        val titles = devices.keys.toTypedArray() //Get names
        btDialog.setTitle("Available devices")
        btDialog.setItems(titles) { dialog, which ->
            //On Click
            //Return bluetooth's address for selected device
            val selectedDevice = devices[titles[which]]!!
            tryConnect(selectedDevice)
            if (titles[which] == Constants.ESP32NAME) {
                view?.showOnlyPhoneStateSwitch(true)
            }
        }
        btDialog.show()
    }


}