package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Message
import com.czterysery.ledcontroller.data.model.MessageQualifier.Companion.CONFIGURATION_PREFIX
import com.czterysery.ledcontroller.data.model.Unknown

class MessageMapper() {
    operator fun invoke(message: String): Message =
        when {
            message.startsWith(CONFIGURATION_PREFIX) -> ConfigurationMapper().map(message)
            else -> Unknown
        }
}