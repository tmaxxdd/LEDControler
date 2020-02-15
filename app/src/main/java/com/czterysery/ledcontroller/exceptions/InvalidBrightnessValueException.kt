package com.czterysery.ledcontroller.exceptions

import java.lang.Exception

class InvalidBrightnessValueException :
    Exception("Brightness value is malformed or out of range (0-255)")