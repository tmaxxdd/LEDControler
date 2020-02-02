package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Illumination.*
import com.czterysery.ledcontroller.exceptions.InvalidIlluminationValueException

class IlluminationMapper {
    operator fun invoke(id: Int) =
        when (id) {
            0 -> NONE
            1 -> PULSE
            2 -> WAVE
            3 -> BEAT
            4 -> SUNRISE
            5 -> UNICORN
            6 -> TROPICAL
            7 -> RELAX
            8 -> ICE
            9 -> RAINBOW
            else -> throw InvalidIlluminationValueException()
        }
}