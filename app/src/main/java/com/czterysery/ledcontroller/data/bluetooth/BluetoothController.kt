package com.czterysery.ledcontroller.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import java.util.HashMap

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