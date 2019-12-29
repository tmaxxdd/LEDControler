package com.czterysery.ledcontroller.view

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.socket.SocketManagerImpl
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_spn.*
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver


class MainActivity : AppCompatActivity(), MainView, ColorObserver {
    private val mPresenter: MainPresenter = MainPresenterImpl(SocketManagerImpl())
    private var connected = false


    //TODO Set text apperance, style the button, add visibility states
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initColorPicker()
        initAnimSpinner()

        brightnessSlider.setOnPositionChangeListener { _, _, _, _, _, newValue ->
            mPresenter.setBrightness(newValue)
        }

        connectionButton.setOnClickListener {
            if (!connected) {
                mPresenter.connectToBluetooth(this) //Very important to get it works
            } else {
                mPresenter.disconnect()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mPresenter.onAttach(this)
    }

    override fun onPause() {
        mPresenter.disconnect()
        mPresenter.onDetach()
        super.onPause()
    }

    override fun onDestroy() {
        colorPicker.unsubscribe(this)
        super.onDestroy()
    }

    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
        mPresenter.setColor(color)
        updateCurrentColor(color)
    }

    override fun updateConnectionState(isConnected: Boolean) {
        this.connected = isConnected
        if (isConnected) {
            mPresenter.loadCurrentParams()
            connectionButton.text = getString(R.string.disconnect)
        } else {
            connectionButton.text = getString(R.string.not_connected)
        }
    }

    override fun updateCurrentColor(receivedColor: Int) {
        row_spn_tv?.textColor = receivedColor
        brightnessSlider.setPrimaryColor(receivedColor)
        connectionButton.setTextColor(receivedColor)
    }

    override fun updateColorBrightnessValue(receivedBrightness: Int) {
        brightnessSlider.setValue(receivedBrightness.toFloat(), true)
    }

    override fun showMessage(text: String) {
        toast(text)
    }

    private fun initColorPicker() {
        colorPicker.subscribe(this)
        colorPicker.setInitialColor(Color.GRAY)
        colorPicker.reset()
    }

    private fun initAnimSpinner() {
        val animAdapter = ArrayAdapter<String>(this, R.layout.row_spn, resources.getStringArray(R.array.animations))
        animAdapter.setDropDownViewResource(R.layout.row_spn_dropdown)
        animationDropdown.adapter = animAdapter
        animationDropdown.setOnItemClickListener { parent, _, position, _ ->
            mPresenter.setAnimation(parent?.adapter?.getItem(position).toString())
            true
        }
    }
}
