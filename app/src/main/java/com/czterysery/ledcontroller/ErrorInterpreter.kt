package com.czterysery.ledcontroller

import com.czterysery.ledcontroller.exceptions.BluetoothNotSupportedException
import com.czterysery.ledcontroller.exceptions.DeviceNotFoundException
import java.io.IOException
import java.util.concurrent.TimeoutException

class ErrorInterpreter {
    operator fun invoke(error: Throwable): Int =
        when (error) {
            is TimeoutException -> R.string.error_timeout
            is IOException -> R.string.error_socket_not_available
            is BluetoothNotSupportedException -> R.string.error_bt_not_available
            is DeviceNotFoundException -> R.string.error_cannot_find_device
            else -> R.string.error_unknown
        }

}