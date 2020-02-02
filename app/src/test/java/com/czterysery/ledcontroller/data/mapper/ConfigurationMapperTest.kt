package com.czterysery.ledcontroller.data.mapper

import org.junit.Test

class ConfigurationMapperTest {
    private val configurationMapper = ConfigurationMapper()

    @Test
    fun `should return value of brightness for 1 up to 3 digits`() {
        val configWithSingleDigit = "CONF=clr:#2233ff,brig:2,illu:1\n"
        val configWithTwoDigits = "CONF=clr:#2233ff,brig:23,illu:1\n"
        val configWithThreeDigits = "CONF=clr:#2233ff,brig:231,illu:1\n"

        assert(configurationMapper.map(configWithSingleDigit).brightness == 2)
        assert(configurationMapper.map(configWithTwoDigits).brightness == 23)
        assert(configurationMapper.map(configWithThreeDigits).brightness == 231)
    }

}