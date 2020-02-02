package com.czterysery.ledcontroller.data.mapper

import android.graphics.Color
import android.util.Log
import com.czterysery.ledcontroller.data.model.Configuration
import com.czterysery.ledcontroller.data.model.Illumination
import com.czterysery.ledcontroller.exceptions.InvalidBrightnessValueException
import com.czterysery.ledcontroller.exceptions.InvalidColorValueException
import com.czterysery.ledcontroller.exceptions.InvalidConfigurationMessageException
import com.czterysery.ledcontroller.exceptions.InvalidIlluminationValueException
import java.lang.Exception

// Example: CONF=clr:#2233ff,brig:231,illu:1\n
const val colorLength = 7 // #2233ff
const val colorPrefix = "clr:"
const val brightnessPrefix = "brig:"
const val illuminationPrefix = "illu:"
private val brightnessRange = 0..255
private val illuminationRange = 0..9

// TODO Write unit test to this class
class ConfigurationMapper {
    private val TAG = "ConfigurationMapper"
    fun map(message: String) =
        Configuration(
            getColor(message),
            getBrightness(message),
            getIllumination(message)
        )

    private fun getColor(message: String): Int =
        if (message.contains("(clr:#).{6}".toRegex())) {
            val start = message.indexOf(colorPrefix) + 4 // Position after 'clr:'
            try {
                Color.parseColor(message.substring(start, start + colorLength))
            } catch (e: Exception) {
                Log.e(TAG, e.message ?: "")
                throw InvalidColorValueException()
            }
        } else {
            throw InvalidConfigurationMessageException()
        }

    private fun getBrightness(message: String): Int =
        if (message.contains("(brig:)".toRegex())) {
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
        if (message.contains("(illu:)".toRegex())) {
            val start = message.indexOf(illuminationPrefix) + 5 // Position after 'illu:'
            message
                .elementAt(start)
                .takeIf { it.isDigit() }
                ?.toInt()
                ?.takeIf { it in illuminationRange }
                ?.let {
                    Illumination.valueOf(it.toString())
                } ?: throw InvalidIlluminationValueException()
        } else {
            throw InvalidConfigurationMessageException()
        }
}