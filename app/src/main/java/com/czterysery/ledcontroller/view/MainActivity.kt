package com.czterysery.ledcontroller.view

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.socket.SocketManagerImpl
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import com.rey.material.widget.Slider
import com.rey.material.widget.Spinner
import com.rey.material.widget.Switch
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver


class MainActivity : AppCompatActivity(), MainView,
        Switch.OnCheckedChangeListener, Slider.OnPositionChangeListener, ColorObserver, View.OnClickListener, Spinner.OnItemClickListener {

    private val mPresenter: MainPresenter = MainPresenterImpl(SocketManagerImpl())
    private var connectionState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        color_picker.subscribe(this)
        color_picker.setInitialColor(Color.WHITE)
        color_picker.reset()
        phone_state.setOnCheckedChangeListener(this)
        brightness_slider.setOnPositionChangeListener(this)
        connection_button.setOnClickListener(this)
        initAnimSpinner()
    }

    override fun onResume() {
        super.onResume()
        mPresenter.onAttach(this)
    }

    override fun onPause() {
        super.onPause()
        mPresenter.disconnect()
        mPresenter.onDetach()
    }

    override fun onColor(color: Int, fromUser: Boolean) {
        mPresenter.setColor(color)
        updateViewColor(color)
    }

    override fun onCheckedChanged(view: Switch?, checked: Boolean) {
        mPresenter.setOnlyPhoneMode(checked)
    }

    override fun onPositionChanged(view: Slider?, fromUser: Boolean, oldPos: Float, newPos: Float, oldValue: Int, newValue: Int) {
        mPresenter.setBrightness(newValue)
    }

    override fun onClick(button: View?) {
        if (!connectionState) {
            mPresenter.connectToBluetooth(this)//Very important to get it works
        } else {
            mPresenter.disconnect()
        }
    }

    override fun onItemClick(parent: Spinner?, view: View?, position: Int, id: Long): Boolean {
        mPresenter.setAnimation(parent?.adapter?.getItem(position).toString())
        showMessage(parent?.adapter?.getItem(position).toString())
        return true
    }

    override fun setConnectionState(connected: Boolean) {
        connectionState = connected
        if (connected) {
            mPresenter.loadCurrentParams()
            connection_button.text = getString(R.string.disconnect)
        } else {
            connection_button.text = getString(R.string.not_connected)
            showOnlyPhoneStateSwitch(false)
        }
    }

    override fun setColorPickerColor(color: Int) {
        color_picker.setInitialColor(color)
    }

    override fun setBrightnessValue(value: Int) {
        brightness_slider.setValue(value.toFloat(), true)
    }

    override fun setOnlyPhoneState(state: Boolean) {
        phone_state.isChecked = state
    }

    override fun showOnlyPhoneStateSwitch(state: Boolean) {
        if (state) {
            phone_state.visibility = View.VISIBLE
            only_phone_title.visibility = View.VISIBLE
        } else {
            phone_state.visibility = View.GONE
            only_phone_title.visibility = View.GONE
        }
    }

    override fun showMessage(text: String) {
        toast(text)
    }

    private fun updateViewColor(color: Int) {
        brightness_slider.setPrimaryColor(color)
        connection_button.setTextColor(color)
    }

    private fun initAnimSpinner() {
        val animAdapter = ArrayAdapter<String>(this, R.layout.row_spn, resources.getStringArray(R.array.animations))
        animAdapter.setDropDownViewResource(R.layout.row_spn_dropdown)
        anim_spinner.adapter = animAdapter
        anim_spinner.setOnItemClickListener(this)
    }
}
