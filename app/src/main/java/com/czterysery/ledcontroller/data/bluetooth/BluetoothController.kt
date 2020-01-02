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

    val adapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    val isSupported = adapter != null

    val isEnabled: Boolean
        get() = adapter?.isEnabled ?: false

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