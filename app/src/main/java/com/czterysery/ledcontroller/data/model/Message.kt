package com.czterysery.ledcontroller.data.model

sealed class Message
data class Configuration(val color: Int, val brightness: Int, val illumination: Illumination) : Message()
object Unknown : Message()