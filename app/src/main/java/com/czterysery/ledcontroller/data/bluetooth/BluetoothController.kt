package com.czterysery.ledcontroller.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import org.jetbrains.anko.AnkoLogger
import java.util.*

/**
 * Created by tmax0 on 27.11.2018.
 */
class BluetoothController: AnkoLogger {
    private val TAG = "BluetoothController"

    fun isEnabled(): Boolean = BluetoothAdapter.getDefaultAdapter().isEnabled

    fun getAdapter(): BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    fun getDeviceAddress(name: String): String? {
        isEnabled().let {
            return getDevices()[name]
        }
    }

    fun getDevices(): HashMap<String, String> {
        val devices = HashMap<String, String>()
        val pairedDevices: Set<BluetoothDevice> = BluetoothAdapter.getDefaultAdapter().bondedDevices
        pairedDevices.forEach { device ->
            devices.put(device.name, device.address)
        }

        return devices
    }

}