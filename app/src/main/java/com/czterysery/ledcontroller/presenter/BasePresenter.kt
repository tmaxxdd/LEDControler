package com.czterysery.ledcontroller.presenter

import com.czterysery.ledcontroller.view.MainView

interface BasePresenter {

    fun onAttach(view: MainView)

    fun onDetach()
}