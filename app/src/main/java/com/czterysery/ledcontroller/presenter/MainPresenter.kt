package com.czterysery.ledcontroller.presenter

interface MainPresenter : BasePresenter {

    fun connect()

    fun disconnect()

    fun setColor(color: Int)

    fun setBrightness(value: Int)

    fun setIllumination(position: Int)

    fun isConnected(): Boolean

    fun isBtEnabled(): Boolean
}