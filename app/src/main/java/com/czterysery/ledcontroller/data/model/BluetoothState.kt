package com.czterysery.ledcontroller.data.model

sealed class BluetoothState
object Enabled : BluetoothState()
object Disabled : BluetoothState()
object NotSupported : BluetoothState()
