package com.czterysery.ledcontroller.presenter

import android.view.View
import com.czterysery.ledcontroller.view.MainView

/**
 * Created by tmax0 on 27.11.2018.
 */
interface BasePresenter {

    fun onAttach(view: MainView)

    fun onDetach()
}