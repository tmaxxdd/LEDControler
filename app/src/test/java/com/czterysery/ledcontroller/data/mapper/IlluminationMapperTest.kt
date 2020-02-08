package com.czterysery.ledcontroller.data.mapper

import com.czterysery.ledcontroller.data.model.Illumination
import com.czterysery.ledcontroller.exceptions.InvalidIlluminationValueException
import org.junit.Test

class IlluminationMapperTest {
    val illuminationMapper = IlluminationMapper()

    @Test
    fun `should return matching illumination type for proper value`() {
        assert(illuminationMapper(0) == Illumination.NONE)
    }

    @Test(expected = InvalidIlluminationValueException::class)
    fun `should throw InvalidIlluminationValueException for inappropriate value`() {
        illuminationMapper(11)
    }

}