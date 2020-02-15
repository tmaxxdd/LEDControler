package com.czterysery.ledcontroller.exceptions

import java.lang.Exception

class InvalidConfigurationMessageException :
    Exception("Part or the whole configuration message is invalid.")