package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Message
import com.czterysery.ledcontroller.data.model.MessageQualifier.Companion.CONFIGURATION_PREFIX
import com.czterysery.ledcontroller.data.model.Unknown

class MessageMapper() {
    private val configurationMapper = ConfigurationMapper()
    operator fun invoke(message: String): Message =
        when {
            message.startsWith(CONFIGURATION_PREFIX) -> configurationMapper(message)
            else -> Unknown
        }
}