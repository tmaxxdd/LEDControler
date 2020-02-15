package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Illumination
import com.czterysery.ledcontroller.exceptions.InvalidBrightnessValueException
import com.czterysery.ledcontroller.exceptions.InvalidColorValueException
import com.czterysery.ledcontroller.exceptions.InvalidConfigurationMessageException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationMapperTest {
    private val configurationMapper = ConfigurationMapper()

    @Test
    fun `should return valid int color value for hex type`() {
        val configWithBlueColor = "CONF=clr:#2233ff,brig:2,illu:1"
        val configWithBlackColor = "CONF=clr:#000000,brig:2,illu:1"
        val configWithWhiteColor = "CONF=clr:#ffffff,brig:2,illu:1"

        assert(configurationMapper(configWithBlueColor).color == -14535681)
        assert(configurationMapper(configWithBlackColor).color == -16777216)
        assert(configurationMapper(configWithWhiteColor).color == -1)
    }

    @Test
    fun `should return value of brightness for 1 up to 3 digits`() {
        val configWithSingleDigit = "CONF=clr:#2233ff,brig:2,illu:1"
        val configWithTwoDigits = "CONF=clr:#2233ff,brig:23,illu:1"
        val configWithThreeDigits = "CONF=clr:#2233ff,brig:231,illu:1"

        assert(configurationMapper(configWithSingleDigit).brightness == 2)
        assert(configurationMapper(configWithTwoDigits).brightness == 23)
        assert(configurationMapper(configWithThreeDigits).brightness == 231)
    }

    @Test
    fun `should return proper illumination by id`() {
        val configWithNoneIllumination = "CONF=clr:#2233ff,brig:2,illu:0"
        val configWithRainbowIllumination = "CONF=clr:#2233ff,brig:2,illu:9"

        assert(configurationMapper(configWithNoneIllumination).illumination == Illumination.NONE)
        assert(configurationMapper(configWithRainbowIllumination).illumination == Illumination.RAINBOW)
    }

    @Test(expected = InvalidConfigurationMessageException::class)
    fun `should throw InvalidConfigurationMessageException for wrong color key`() {
        val configWithErrorInColor = "CONF=colr:#2233ff,brig:2,illu:1"
        configurationMapper(configWithErrorInColor)

    }

    @Test(expected = InvalidConfigurationMessageException::class)
    fun `should throw InvalidConfigurationMessageException for wrong brightness key`() {
        val configWithErrorInBrightness = "CONF=colr:#2233ff,brigh:2,illu:1"
        configurationMapper(configWithErrorInBrightness)
    }

    @Test(expected = InvalidConfigurationMessageException::class)
    fun `should throw InvalidConfigurationMessageException for wrong illumination key`() {
        val configWithErrorInIllumination = "CONF=colr:#2233ff,brig:2,ilu:1"
        configurationMapper(configWithErrorInIllumination)
    }

    @Test(expected = InvalidColorValueException::class)
    fun `should throw InvalidColorValueException for shortened color`() {
        val configWithShortenedColor = "CONF=clr:#23f,brig:2,illu:1"
        configurationMapper(configWithShortenedColor)
    }

    @Test(expected = InvalidBrightnessValueException::class)
    fun `should throw InvalidBrightnessValueException for brightness over the range`() {
        val configWithBrightnessOutOfRange = "CONF=clr:#2233ff,brig:256,illu:1"
        configurationMapper(configWithBrightnessOutOfRange)
    }

    @Test(expected = InvalidBrightnessValueException::class)
    fun `should throw InvalidBrightnessValueException for brightness below the range`() {
        val configWithBrightnessOutOfRange = "CONF=clr:#2233ff,brig:-1,illu:1"
        configurationMapper(configWithBrightnessOutOfRange)
    }

    @Test
    fun `should throw InvalidIlluminationValueException for type out of range`() {
        val configurationWithIlluminationOutOfRange = "CONF=clr:#2233ff,brig:231,illu:10"
        configurationMapper(configurationWithIlluminationOutOfRange)
    }
}