package com.czterysery.ledcontroller.view

import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.DialogManager
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.mapper.MessageMapper
import com.czterysery.ledcontroller.data.model.*
import com.czterysery.ledcontroller.data.socket.SocketManagerImpl
import com.czterysery.ledcontroller.presenter.MainPresenter
import com.czterysery.ledcontroller.presenter.MainPresenterImpl
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_spn.*
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import top.defaults.colorpicker.ColorObserver
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity(), MainView, ColorObserver {
    private lateinit var dialogManager: DialogManager
    private val btStateReceiver = BluetoothStateBroadcastReceiver()
    private val mPresenter: MainPresenter = MainPresenterImpl(
        btStateReceiver,
        BluetoothController(),
        SocketManagerImpl(),
        MessageMapper()
    )

    private var allowChangeColor = false
    private var previousConnectionState: ConnectionState = Disconnected

    private val illuminationAdapter by lazy {
        ArrayAdapter<String>(this, R.layout.row_spn, Illumination.values().map { it.name })
            .apply { setDropDownViewResource(R.layout.row_spn_dropdown) }
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

    override fun updateColor(receivedColor: Int) {
        allowChangeColor = false
        colorPicker.setInitialColor(receivedColor)
        allowChangeColor = true
        adjustViewColor(receivedColor)
    }

    override fun updateBrightness(receivedBrightness: Int) {
        brightnessSlider.setValue(receivedBrightness.toFloat(), true)
    }

    // TODO Repair this. View is recreating for unknown reason
    override fun updateIllumination(receivedIllumination: Illumination) {
        val position = Illumination.values().indexOf(receivedIllumination)
        illuminationDropdown.setSelection(position)
    }

    private fun adjustViewColor(color: Int) {
        dropdownItem.setTextColor(color)
        brightnessSlider.setPrimaryColor(color)
        connectAction.setTextColor(color)
    }

    private fun changeConnectionStatus() {
        if (!mPresenter.isConnected()) {
            mPresenter.connect()
        } else {
            mPresenter.disconnect()
        }
    }

    override fun showMessage(text: String) {
        toast(text)
    }

    override fun showLoading(shouldShow: Boolean) {
        if (shouldShow)
            dialogManager.loading.show()
        else
            dialogManager.loading.dismiss()
    }

    override fun showDevicesList(devices: Array<String>, selectedDevice: (DialogInterface, String) -> Unit) {
        dialogManager.deviceSelection(devices, selectedDevice)
            .show()
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

    private fun showReconnect() {
        with(dialogManager.reconnect) {
            positiveActionClickListener {
                if (mPresenter.isBtEnabled()) mPresenter.connect()
                dismiss()
            }
            show()
        }
    }

    private fun showBottomMessage(@StringRes messageId: Int, vararg args: Any) {
        Snackbar.make(container, getString(messageId, args), Snackbar.LENGTH_SHORT).show()
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
