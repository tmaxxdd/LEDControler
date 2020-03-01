package com.czterysery.ledcontroller.view

import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.Constants.Companion.REPO_URL
import com.czterysery.ledcontroller.DialogManager
import com.czterysery.ledcontroller.ErrorInterpreter
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.mapper.MessageMapper
import com.czterysery.ledcontroller.data.model.Connected
import com.czterysery.ledcontroller.data.model.ConnectionState
import com.czterysery.ledcontroller.data.model.Disconnected
import com.czterysery.ledcontroller.data.model.Illumination
import com.czterysery.ledcontroller.data.socket.BluetoothSocketManager
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.brightnessSlider
import kotlinx.android.synthetic.main.activity_main.colorPicker
import kotlinx.android.synthetic.main.activity_main.connectAction
import kotlinx.android.synthetic.main.activity_main.container
import kotlinx.android.synthetic.main.activity_main.githubAction
import kotlinx.android.synthetic.main.activity_main.illuminationDropdown
import kotlinx.android.synthetic.main.row_spn.dropdownItem
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity(), MainView, ColorObserver {
    private lateinit var dialogManager: DialogManager
    private val btStateReceiver = BluetoothStateBroadcastReceiver()
    private val mPresenter: MainPresenter = MainPresenterImpl(
        btStateReceiver,
        BluetoothController(),
        BluetoothSocketManager(),
        MessageMapper(),
        ErrorInterpreter()
    )

    private var allowChangeColor = false
    private var previousConnectionState: ConnectionState = Disconnected

    private val illuminationAdapter by lazy {
        ArrayAdapter<String>(this, R.layout.row_spn, Illumination.values().map { it.name })
            .apply { setDropDownViewResource(R.layout.row_spn_dropdown) }
    }

    private val githubIntent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(REPO_URL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialogManager = DialogManager(this)

        initColorPicker()
        initIlluminationDropdown()

        brightnessSlider.setOnPositionChangeListener { _, _, _, _, _, newValue ->
            mPresenter.setBrightness(newValue)
        }

        connectAction.setOnClickListener {
            changeConnectionStatus()
        }

        githubAction.setOnClickListener {
            startActivity(githubIntent)
        }

        registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onStart() {
        super.onStart()
        mPresenter.onAttach(this)
    }

    override fun onStop() {
        mPresenter.disconnect()
        mPresenter.onDetach()
        showReconnect()
        super.onStop()
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
            adjustViewColor(color)
        }
    }

    override fun updateColor(receivedColor: Int) {
        // Don't execute code in onColor method
        allowChangeColor = false
        colorPicker.setInitialColor(receivedColor)
        allowChangeColor = true
        adjustViewColor(receivedColor)
    }

    override fun updateBrightness(receivedBrightness: Int) {
        brightnessSlider.setValue(receivedBrightness.toFloat(), true)
    }

    override fun updateIllumination(receivedIllumination: Illumination) {
        illuminationDropdown.setSelection(receivedIllumination.ordinal)
    }

    override fun showMessage(text: String) {
        toast(text)
    }

    override fun showLoading(shouldShow: Boolean) {
        if (shouldShow)
            dialogManager.loading.show()
        else
            dialogManager.loading.hide()
    }

    override fun showDevicesList(devices: Array<String>, selectedDevice: (DialogInterface, String) -> Unit) {
        dialogManager.deviceSelection(devices, selectedDevice).show()
    }

    override fun showPairWithDevice() {
        with(dialogManager.pairWithDevice) {
            positiveActionClickListener { dismiss() }
            show()
        }
    }

    override fun showConnected(device: String) {
        updateConnectionViewState(isConnected = true)

        showBottomMessage(R.string.connected_with, device)
        previousConnectionState = Connected(device)
    }

    override fun showDisconnected() {
        updateConnectionViewState(isConnected = false)

        if (previousConnectionState is Connected)
            showBottomMessage(R.string.disconnected)

        previousConnectionState = Disconnected
    }

    override fun showError(@StringRes messageId: Int, vararg args: Any) {
        showBottomMessage(messageId, args)
    }

    override fun showBtEnabled() {
        dialogManager.enableBT.dismiss()
    }

    override fun showBtDisabled() {
        with(dialogManager.enableBT) {
            positiveActionClickListener { runBtEnabler() }
            negativeActionClickListener { dismiss() }
            show()
        }
    }

    override fun showBtNotSupported() {
        dialogManager.btNotSupported
            .positiveActionClickListener { finish() }
            .show()
    }

    private fun changeConnectionStatus() {
        if (!mPresenter.isConnected()) {
            mPresenter.connect()
        } else {
            mPresenter.disconnect()
        }
    }

    private fun updateConnectionViewState(isConnected: Boolean) {
        if (isConnected) {
            colorPicker.alpha = 1f
            illuminationDropdown.isEnabled = true
            brightnessSlider.isEnabled = true
            allowChangeColor = true
            connectAction.text = getString(R.string.disconnect)
        } else {
            colorPicker.alpha = 0.5f
            illuminationDropdown.isEnabled = false
            brightnessSlider.isEnabled = false
            allowChangeColor = false
            connectAction.text = getString(R.string.connect)
        }
    }

    private fun adjustViewColor(color: Int) {
        dropdownItem.setTextColor(color)
        brightnessSlider.setPrimaryColor(color)
        connectAction.setTextColor(color)
    }

    private fun showReconnect() {
        with(dialogManager.reconnect) {
            positiveActionClickListener {
                if (mPresenter.isBtEnabled()) {
                    mPresenter.connect()
                }
                dismiss()
            }
            show()
        }
    }

    private fun showBottomMessage(@StringRes messageId: Int, vararg args: Any) {
        Snackbar.make(container,
            getString(messageId, args.map { it.toString() }.takeIf { it.isNotEmpty() }?.first()),
            Snackbar.LENGTH_SHORT
        ).show()
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

    private fun initIlluminationDropdown() {
        with(illuminationDropdown) {
            adapter = illuminationAdapter
            setOnItemSelectedListener { _, _, position, _ ->
                mPresenter.setIllumination(position)
            }
        }
    }
}
