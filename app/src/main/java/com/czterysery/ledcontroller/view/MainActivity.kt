package com.czterysery.ledcontroller.view

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.DialogManager
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.model.*
import com.czterysery.ledcontroller.data.socket.SocketManagerImpl
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_spn.*
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity(), MainView, ColorObserver {
    private val TAG = "LEDController"

    private lateinit var dialogManager: DialogManager
    private val btStateReceiver = BluetoothStateBroadcastReceiver()
    private val mPresenter: MainPresenter = MainPresenterImpl(
            btStateReceiver,
            BluetoothController(),
            SocketManagerImpl()
    )

    private val bluetoothStateListener: (state: BluetoothState) -> Unit = { state: BluetoothState ->
        when (state) {
            Enabled -> showBtEnabled()
            Disabled -> showBtDisabled()
            NotSupported -> showBtNotSupported()
            None -> // When app starts, BT is in previous state. Check state manually.
                if (mPresenter.isBtEnabled()) showBtEnabled() else showBtDisabled()
        }
    }

    private val connectionStateListener: (state: ConnectionState) -> Unit = { state ->
        when (state) {
            is Connected -> showConnected(state.device)
            Disconnected -> showDisconnected()
            is Error -> showError(state.message)
        }
    }

    private var allowChangeColor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialogManager = DialogManager(this)

        initColorPicker()
        initAnimSpinner()

        brightnessSlider.setOnPositionChangeListener { _, _, _, _, _, newValue ->
            mPresenter.setBrightness(newValue)
        }

        connectionButton.setOnClickListener {
            if (mPresenter.isBtEnabled()) {
                changeConnectionStatus()
            } else {
                showBtDisabled()
            }
        }

        mPresenter.setBluetoothStateListener(bluetoothStateListener)
        registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        mPresenter.setConnectionStateListener(connectionStateListener)
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
        unregisterReceiver(btStateReceiver)
        colorPicker.unsubscribe(this)
        dialogManager.dismissAll()
        super.onDestroy()
    }

    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
        if (allowChangeColor) {
            mPresenter.setColor(color)
            updateCurrentColor(color)
        }
    }

    override fun updateConnectionState(isConnected: Boolean) {
        if (isConnected) {
            mPresenter.loadCurrentParams()
            connectionButton.text = getString(R.string.disconnect)
        } else {
            connectionButton.text = getString(R.string.connect)
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

    private fun changeConnectionStatus() {
        if (mPresenter.isConnected()) {
            mPresenter.connect(this)
        } else {
            mPresenter.disconnect()
        }
    }

    // TODO Add updateCurrentAnimation

    override fun showMessage(text: String) {
        toast(text)
    }

    private fun showConnected(device: String) {
        // TODO Show snackbar with device name
        setViewsEnabled(true)
    }

    private fun showDisconnected() {
        // TODO Show snackbar with device name
        setViewsEnabled(false)
    }

    private fun showError(message: String) {
        setViewsEnabled(false)
    }

    private fun showBtEnabled() {
        dialogManager.enableBT.dismiss()
    }

    private fun showBtDisabled() {
        mPresenter.disconnect()
        with(dialogManager.enableBT) {
            positiveActionClickListener { runBtEnabler() }
            negativeActionClickListener { this.dismiss() }
            show()
        }
    }

    private fun showBtNotSupported() {
        dialogManager.btNotSupported
                .positiveActionClickListener { finish() }
                .show()
    }

    private fun setViewsEnabled(enabled: Boolean) {
        if (enabled) {
            colorPicker.alpha = 1f
            animationDropdown.isEnabled = true
            brightnessSlider.isEnabled = true
            allowChangeColor = true
        } else {
            colorPicker.alpha = 0.5f
            animationDropdown.isEnabled = false
            brightnessSlider.isEnabled = false
            allowChangeColor = false
        }
    }

    private fun runBtEnabler() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
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
