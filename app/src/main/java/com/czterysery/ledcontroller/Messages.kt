package com.czterysery.ledcontroller

class Messages {

    companion object {
        /* Receiving a data */
        val GET_ONLY_PHONE_MODE = "GET_ONLY_PHONE_MODE"
        val GET_COLOR = "GET_COLOR"
        val GET_BRIGHTNESS = "GET_BRIGHTNESS"
        /* Sending a data */
        const val CONNECTED = "CONNECTED"
        const val DISCONNECTED = "DISCONNECTED"
        const val SET_ONLY_PHONE_MODE = "SET_ONLY_PHONE_MODE_"
        const val SET_COLOR = "SET_COLOR_"
        const val SET_BRIGHTNESS = "SET_BRIGHTNESS_"
        const val SET_ANIMATION = "SET_ANIM_"
        /* Returning a data */
        const val ANIMATION_IS = "ANIMATION_IS_"
        val ONLY_PHONE_MODE_IS = "ONLY_PHONE_MODE_IS_"
        val COLOR_IS = "COLOR_IS_"
        val BRIGHTNESS_IS = "BRIGHTNESS_IS_"
    }

}