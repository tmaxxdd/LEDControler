package com.czterysery.ledcontroller.data.model

sealed class ConnectionState
data class Connected(val device: String) : ConnectionState()
object Disconnected : ConnectionState()
data class Error(val messageId: Int) : ConnectionState()