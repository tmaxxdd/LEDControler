package com.czterysery.ledcontroller.data.mapper

import android.graphics.Color
import com.czterysery.ledcontroller.data.model.Configuration
import com.czterysery.ledcontroller.data.model.Illumination
import com.czterysery.ledcontroller.exceptions.InvalidBrightnessValueException
import com.czterysery.ledcontroller.exceptions.InvalidColorValueException
import com.czterysery.ledcontroller.exceptions.InvalidConfigurationMessageException
import com.czterysery.ledcontroller.exceptions.InvalidIlluminationValueException

const val colorLength = 7 // #2233ff
const val colorPrefix = "clr:"
const val brightnessPrefix = "brig:"
const val illuminationPrefix = "illu:"
private val brightnessRange = 0..255
private val illuminationRange = 0..9

class ConfigurationMapper {
    private val TAG = "ConfigurationMapper"
    private val illuminationMapper = IlluminationMapper()

    operator fun invoke(message: String) =
        Configuration(
            getColor(message),
            getBrightness(message),
            getIllumination(message)
        )

    private fun getColor(message: String): Int =
        if (message.contains("(clr:#).{6}".toRegex())) {
            val start = message.indexOf(colorPrefix) + 4 // Position after 'clr:'
            val colorVal = message.substring(start, start + colorLength)
            try {
                Color.parseColor(colorVal)
            } catch (e: Exception) {
                throw InvalidColorValueException(colorVal)
            }
        } else {
            throw InvalidConfigurationMessageException()
        }

    private fun getBrightness(message: String): Int =
        if (message.contains("($brightnessPrefix)".toRegex())) {
            val start = message.indexOf(brightnessPrefix) + 5 // Position after 'brig:'
            // Filter only first digits after prefix
            message
                .substring(start) // "brig:244,,"
                .takeWhile { it.isDigit() } // "244,," -> "244"
                .toInt() // "244" -> 244
                .takeIf { it in brightnessRange }
                ?: throw InvalidBrightnessValueException()
        } else {
            throw InvalidConfigurationMessageException()
        }

    private fun getIllumination(message: String): Illumination =
        if (message.contains("($illuminationPrefix)".toRegex())) {
            val start = message.indexOf(illuminationPrefix) + 5 // Position after 'illu:'
            message
                .elementAt(start)
                .takeIf { it.isDigit() }
                ?.toString()?.toInt()
                ?.takeIf { it in illuminationRange }
                ?.let { illuminationId ->
                    return@let illuminationMapper(illuminationId)
                } ?: throw InvalidIlluminationValueException()
        } else {
            throw InvalidConfigurationMessageException()
        }
}