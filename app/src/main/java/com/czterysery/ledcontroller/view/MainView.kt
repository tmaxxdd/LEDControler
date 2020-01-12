package com.czterysery.ledcontroller.view

import android.content.DialogInterface

interface MainView {

    fun updateCurrentColor(receivedColor: Int)

    fun updateColorBrightnessValue(receivedBrightness: Int)

    fun showMessage(text: String)

    fun showLoading(shouldShow: Boolean = true)

    fun showDevicesList(devices: Array<String>, selectedDevice: (DialogInterface, String) -> Unit)

    fun showPairWithDevice()

    fun showConnected(device: String)

    fun showDisconnected()

    fun showError(message: String)

    fun showBtEnabled()

    fun showBtDisabled()

    fun showBtNotSupported()
}