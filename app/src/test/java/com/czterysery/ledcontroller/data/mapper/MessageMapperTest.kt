package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Configuration
import com.czterysery.ledcontroller.data.model.Unknown
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageMapperTest {
    private val messageMapper = MessageMapper()

    @Test
    fun `should return message type Configuration when message starts with CONFIGURATION_PREFIX`() {
        val configurationMessage = "CONF=clr:#2233ff,brig:2,illu:1"
        assert(messageMapper(configurationMessage) is Configuration)
    }

    @Test
    fun `should return message type Unknown when prefix is malformed`() {
        val malformedMessage = "CO.NF=clr:#2233ff,brig:2,illu:1"
        assert(messageMapper(malformedMessage) is Unknown)
    }

    @Test
    fun `should return message type Unknown when prefix is not known`() {
        val malformedMessage = "WRONG=clr:#2233ff,brig:2,illu:1"
        assert(messageMapper(malformedMessage) is Unknown)
    }

}