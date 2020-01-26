package com.czterysery.ledcontroller.exceptions

import java.lang.Exception

class InvalidColorValueException :
    Exception("Color value is malformed or unknown. Define it as a hex triplet.")