package com.czterysery.ledcontroller.view

import android.content.BroadcastReceiver
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.BluetoothState
import com.czterysery.ledcontroller.data.model.Disabled
import com.czterysery.ledcontroller.data.model.Enabled
import com.czterysery.ledcontroller.data.model.NotSupported
import com.czterysery.ledcontroller.data.socket.SocketManagerImpl
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import kotlinx.android.synthetic.main.activity_main.animationDropdown
import kotlinx.android.synthetic.main.activity_main.brightnessSlider
import kotlinx.android.synthetic.main.activity_main.colorPicker
import kotlinx.android.synthetic.main.activity_main.connectionButton
import kotlinx.android.synthetic.main.row_spn.dropdownItem
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver

class MainActivity : AppCompatActivity(), MainView, ColorObserver {
    private val mPresenter: MainPresenter = MainPresenterImpl(BluetoothController(), SocketManagerImpl())
    private var connected = false

    private val bluetoothStateListener = { state: BluetoothState ->
        when (state) {
            Enabled -> showMessage("Enabled")
            Disabled -> showMessage("Disabled")
            NotSupported -> showMessage("Not supported")
        }
    }

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
                mPresenter.connectToBluetooth(this)
            } else {
                mPresenter.disconnect()
            }
        }

        mPresenter.setBluetoothStateListener(bluetoothStateListener)
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
        dropdownItem?.textColor = receivedColor
        brightnessSlider.setPrimaryColor(receivedColor)
        connectionButton.setTextColor(receivedColor)
    }

    // TODO Change name to updateCurrentBrightness
    override fun updateColorBrightnessValue(receivedBrightness: Int) {
        brightnessSlider.setValue(receivedBrightness.toFloat(), true)
    }

    // TODO Add updateCurrentAnimation

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
