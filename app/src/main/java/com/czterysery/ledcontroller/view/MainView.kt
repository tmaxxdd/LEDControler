package com.czterysery.ledcontroller.view

interface MainView {

    fun updateCurrentColor(receivedColor: Int)

    fun updateColorBrightnessValue(receivedBrightness: Int)

    fun updateConnectionState(isConnected: Boolean)

    fun showMessage(text: String)

    fun showLoading()

    fun showDevicesList(devices: Array<String>, selectedDevice: (String) -> Unit)

    fun showPairWithDevice()
}