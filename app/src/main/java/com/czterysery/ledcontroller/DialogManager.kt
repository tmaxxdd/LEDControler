package com.czterysery.ledcontroller

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.rey.material.app.Dialog
import com.rey.material.app.SimpleDialog

class DialogManager(private val context: Context) {

    val loading: Dialog = Dialog(context, R.style.CustomDialog)
            .title(R.string.connecting_title)
            .contentView(R.layout.dialog_loading_view)
            .cancelable(false)

    val btNotSupported: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.bt_not_supported_message)
            .title(R.string.bt_not_supported_title)
            .positiveAction(R.string.close_app)
            .cancelable(false)

    val enableBT: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.enable_bt_message)
            .title(R.string.bluetooth_disabled)
            .positiveAction(R.string.turn_on)
            .negativeAction(R.string.cancel)
            .cancelable(true)

    val reconnect: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.not_connected_to_device)
            .title(R.string.disconnected)
            .positiveAction(R.string.reconnect)
            .cancelable(true)

    val pairWithDevice: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.not_paired_with_device)
            .title(R.string.no_devices)
            .positiveAction(R.string.ok)
            .cancelable(true)

    fun deviceSelection(devices: Array<String>, selectedDevice: (DialogInterface, String) -> Unit): AlertDialog =
            AlertDialog.Builder(context)
                    .setTitle(R.string.available_devices_title)
                    .setItems(devices) { dialog, selected: Int ->
                        selectedDevice(dialog, devices[selected])
                        dialog.dismiss()
                    }.create()

    fun hideAll() {
        btNotSupported.dismiss()
        enableBT.dismiss()
        reconnect.dismiss()
        loading.dismiss()
    }

    fun dismissAll() {
        btNotSupported.dismissImmediately()
        enableBT.dismissImmediately()
        reconnect.dismissImmediately()
        loading.dismissImmediately()
    }
}