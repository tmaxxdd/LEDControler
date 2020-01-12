package com.czterysery.ledcontroller.presenter

interface MainPresenter : BasePresenter {

    fun connect()

    fun disconnect()

    fun setColor(color: Int)

    fun setBrightness(value: Int)

    fun setAnimation(anim: String)

    fun isConnected(): Boolean

    fun isBtEnabled(): Boolean
}