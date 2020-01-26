package com.czterysery.ledcontroller.view

import android.content.DialogInterface
import androidx.annotation.StringRes

interface MainView {

    fun updateCurrentColor(receivedColor: Int)

    fun updateColorBrightnessValue(receivedBrightness: Int)

    fun showMessage(text: String)

    fun showLoading(shouldShow: Boolean = true)

    fun showDevicesList(devices: Array<String>, selectedDevice: (DialogInterface, String) -> Unit)

    fun showPairWithDevice()

    fun showConnected(device: String)

    fun showDisconnected()

    fun showError(@StringRes messageId: Int, vararg args: Any)

    fun showBtEnabled()

    fun showBtDisabled()

    fun showBtNotSupported()
}