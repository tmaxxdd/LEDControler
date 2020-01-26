package com.czterysery.ledcontroller.data.mapper

import android.util.JsonReader
import com.czterysery.ledcontroller.data.model.Configuration
import com.squareup.moshi.Moshi
import org.json.JSONObject

// CONF=clr:#2233ff,brig:231,illu:1\n
const val colorLength = 7 // #2233ff
const val colorPrefix = "clr:"
const val brightnessPrefix = "brig:"
const val illuminationPrefix = "illu:"
val brightnessRange = 0..255

// TODO Read about moshi and implement https://github.com/square/moshi
// TODO Write unit tests
class ConfigurationMapper {
    fun map(message: String): Configuration =


//    private fun getColor(message: String): Int {
//        if (message.contains("(clr:#).{6}".toRegex())) {
//            val start = message.indexOf(colorPrefix) + 4 // end of 'clr:'
//            return try {
//                Color.parseColor(message.substring(start, start + (colorLength)))
//            } catch (e: Exception) {
//                throw InvalidColorValueException()
//            }
//        } else {
//            throw InvalidConfigurationMessageException()
//        }
//    }
//
//    private fun getBrightness(message: String): Int {
//        if (message.contains("(brig:)".toRegex())) {
//            val start = message.indexOf(brightnessPrefix) + 5 // end of 'brig:'
//            while (message[start] is Number)
//            return if (color in brightnessRange)
//                color
//            else
//                throw InvalidBrightnessValueException()
//        } else {
//            throw InvalidConfigurationMessageException()
//        }
//    }
//
//    private fun getIllumination() {
//
//    }


}