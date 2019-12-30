package com.czterysery.ledcontroller.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import java.util.*
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.czterysery.ledcontroller.data.model.BluetoothState
import com.czterysery.ledcontroller.data.model.Disabled
import com.czterysery.ledcontroller.data.model.Enabled
import com.czterysery.ledcontroller.data.model.NotSupported


class BluetoothController {

    private var bluetoothStateListener: ((state: BluetoothState) -> Unit)? = null

    val adapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    private val isSupported = adapter != null

    val isEnabled: Boolean
        get() = adapter?.isEnabled ?: false

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_ON -> bluetoothStateListener?.invoke(Enabled)
                    BluetoothAdapter.STATE_OFF -> bluetoothStateListener?.invoke(Disabled)
                    BluetoothAdapter.ERROR -> bluetoothStateListener?.invoke(NotSupported)
                }
            }
        }
    }

    fun setBluetoothStateListener(listener: (state: BluetoothState) -> Unit): BroadcastReceiver {
        if (!isSupported) listener.invoke(NotSupported)
        bluetoothStateListener = listener
        return mReceiver
    }

    fun getDeviceAddress(name: String): String? {
        isEnabled.let {
            return getDevices()[name]
        }
    }

    fun getDevices(): HashMap<String, String> {
        val devices = HashMap<String, String>()
        val pairedDevices: Set<BluetoothDevice> = adapter?.bondedDevices ?: emptySet()
        pairedDevices.forEach { device ->
            devices[device.name] = device.address
        }

        return devices
    }
}