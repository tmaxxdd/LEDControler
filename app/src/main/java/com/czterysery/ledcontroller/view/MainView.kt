package com.czterysery.ledcontroller.view

import android.content.DialogInterface
import android.view.View
import androidx.annotation.StringRes
import com.czterysery.ledcontroller.data.model.Illumination

interface MainView {

    fun updateColor(receivedColor: Int)

    fun updateBrightness(receivedBrightness: Int)

    fun updateIllumination(receivedIllumination: Illumination)

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