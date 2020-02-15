package com.czterysery.ledcontroller.exceptions

import java.lang.Exception

class InvalidColorValueException(color: String) :
    Exception("Color value: $color is malformed or unknown. Define it as a hex triplet.")