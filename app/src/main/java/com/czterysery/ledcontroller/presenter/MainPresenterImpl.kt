package com.czterysery.ledcontroller.presenter

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.czterysery.ledcontroller.BluetoothStateBroadcastReceiver
import com.czterysery.ledcontroller.Messages
import com.czterysery.ledcontroller.Messages.Companion.END_OF_LINE
import com.czterysery.ledcontroller.R
import com.czterysery.ledcontroller.data.bluetooth.BluetoothController
import com.czterysery.ledcontroller.data.mapper.MessageMapper
import com.czterysery.ledcontroller.data.model.*
import com.czterysery.ledcontroller.data.socket.SocketManager
import com.czterysery.ledcontroller.view.MainView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jetbrains.anko.Android
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

const val RESPONSE_TIMEOUT = 2L // sec
val IGNORE_SUCCESS = {}

class MainPresenterImpl(
    private val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver,
    private val btController: BluetoothController,
    private val socketManager: SocketManager,
    private val messageMapper: MessageMapper
) : MainPresenter {
    private val TAG = "MainPresenter"

    private val bluetoothStateListener: (state: BluetoothState) -> Unit = { state: BluetoothState ->
        onBtStateChanged(state)
    }

    private val connectionStateListener: (state: ConnectionState) -> Unit = { state ->
        onConnectionStateChanged(state)
    }

    private var btStateDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null
    private var connectionDisposable: Disposable? = null
    private var messagePublisherDisposable: Disposable? = null
    private var messageWriterDisposable: Disposable? = null
    private var configurationListenerDisposable: Disposable? = null

    private var view: MainView? = null

    override fun onAttach(view: MainView) {
        this.view = view
        registerListeners()
    }

    override fun onDetach() {
        this.view = null
        disposeAll()
    }

    override fun connect() {
        if (isBtEnabled()) {
            showDevicesAndTryConnect()
        } else {
            view?.showBtDisabled()
        }
    }

    override fun disconnect() {
        sendConnectionMessage(connected = false)
        socketManager.disconnect().subscribe(
            IGNORE_SUCCESS, { error ->
            if (error is TimeoutException)
                socketManager.connectionState.onNext(Error(R.string.error_disconnect))
            else
                Log.e(TAG, "Couldn't close the socket: $error")
        })
    }

    override fun setColor(color: Int) {
        val hexColor = String.format("#%06X", (0xFFFFFF and color))
        writeMessage(Messages.SET_COLOR + hexColor)
    }

    override fun setBrightness(value: Int) {
        writeMessage(Messages.SET_BRIGHTNESS + value)
    }

    override fun setIllumination(position: Int) {
        writeMessage(Messages.SET_ILLUMINATION + position)
    }

    override fun isConnected() = socketManager.connectionState.value is Connected

    override fun isBtEnabled(): Boolean = btController.isEnabled

    private fun subscribeConnectionStateListener(listener: (state: ConnectionState) -> Unit) {
        connectionStateDisposable?.dispose()
        connectionStateDisposable = socketManager.connectionState
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { state -> listener(state) },
                { error ->
                    // TODO Create one class with error messages
                    if (error is TimeoutException)
                        socketManager.connectionState.onNext(Error(R.string.error_timeout))
                    else
                        Log.e(TAG, "Error during observing connection state: $error")
                }
            )
    }

    private fun subscribeBluetoothStateListener(listener: (state: BluetoothState) -> Unit) {
        btStateDisposable?.dispose()
        btStateDisposable = bluetoothStateBroadcastReceiver.btState
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { state -> checkIfBtSupportedAndReturnState(listener, state) },
                { error -> Log.e(TAG, "Error during observing BT state: $error") }
            )
    }

    private fun subscribeMessagePublisher() {
        messagePublisherDisposable?.dispose()
        messagePublisherDisposable = socketManager.messagePublisher
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { message -> parseMessage(message) },
                { view?.showError(R.string.error_receiving_message) }
            )
    }

    private fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
            is Connected -> {
                view?.showConnected(state.device)
                subscribeMessagePublisher()
                tryToGetConfiguration()
            }
            Disconnected -> view?.showDisconnected()
            is Error -> view?.showError(state.messageId)
        }
        view?.showLoading(shouldShow = false)
    }

    private fun onBtStateChanged(state: BluetoothState) {
        when (state) {
            Enabled -> view?.showBtEnabled()
            Disabled -> {
                socketManager.connectionState.onNext(Disconnected)
                view?.showBtDisabled()
            }
            NotSupported -> view?.showBtNotSupported()
            None -> // When app starts, BT is in previous state. Check state manually.
                if (isBtEnabled()) view?.showBtEnabled() else view?.showBtDisabled()
        }
    }

    private fun writeMessage(message: String) {
        messageWriterDisposable?.dispose()
        messageWriterDisposable = socketManager.writeMessage(message + END_OF_LINE)
            .subscribe(IGNORE_SUCCESS, { error ->
                Log.e(TAG, "Couldn't write $message, $error")
            })
    }

    private fun sendConnectionMessage(connected: Boolean) {
        if (connected)
            writeMessage(Messages.CONNECTED)
        else
            writeMessage(Messages.DISCONNECTED)
    }

    private fun checkIfBtSupportedAndReturnState(listener: (state: BluetoothState) -> Unit, state: BluetoothState) {
        if (btController.isSupported.not())
            listener(NotSupported)
        else
            listener(state)
    }

    private fun showDevicesAndTryConnect() {
        view?.let {
            val devices = btController.getDevices().keys.toTypedArray()
            if (devices.isNotEmpty()) {
                it.showLoading()
                it.showDevicesList(devices) { dialog, deviceName ->
                    // On selected device
                    dialog.dismiss()
                    tryToConnectWithDevice(deviceName)
                }
            } else {
                it.showPairWithDevice()
            }
        }
    }

    private fun tryToConnectWithDevice(deviceName: String) {
        when {
            btController.adapter == null ->
                socketManager.connectionState.onNext(Error(R.string.error_bt_not_available))

            btController.getDeviceAddress(deviceName) == null ->
                socketManager.connectionState.onNext(Error(R.string.error_cannot_find_device))

            else -> {
                // TODO Fix sending connected message (some delay may help)
                connectionDisposable?.dispose()
                connectionDisposable = socketManager.connect(
                    btController.getDeviceAddress(deviceName) as String,
                    btController.adapter as BluetoothAdapter
                ).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { view?.showLoading() }
                    .doOnTerminate { view?.showLoading(shouldShow = false) }
                    .doOnComplete { sendConnectionMessage(connected = true) }
                    .subscribe({
                        tryToGetConfiguration()
                    }, { error ->
                        Log.e(TAG, "Couldn't connect to device: $error")
                    })
            }
        }
    }

    private fun tryToGetConfiguration() {
        view?.showLoading()
        configurationListenerDisposable?.dispose()
        configurationListenerDisposable = Completable.fromCallable {
            writeMessage(Messages.GET_CONFIGURATION)
        }.andThen(awaitForResponse())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate { view?.showLoading(false) }
            .retry(5)
            .subscribe(
                IGNORE_SUCCESS,
                { error ->
                    if (error is TimeoutException) {
                        Log.e(TAG, "Timeout! Configuration message hasn't been delivered on time.")
                    } else {
                        Log.e(TAG, "Error during receiving config. message: $error")
                    }
                }
            )
    }

    private fun awaitForResponse() = socketManager.messagePublisher
        .subscribeOn(Schedulers.io())
        .flatMapCompletable {
            if (messageMapper(it) is Configuration)
                Completable.complete()
            else
                Completable.never()
        }.timeout(RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)

    private fun parseMessage(messageValue: String) {
        when (val message: Message = messageMapper(messageValue)) {
            is Configuration -> {
                configurationListenerDisposable?.dispose()
                view?.showLoading(false)
                adjustViewToConfiguration(message)
            }
            is Unknown -> {
            }
        }
    }

    private fun adjustViewToConfiguration(config: Configuration) {
        view?.let { view ->
            with(view) {
                updateColor(config.color)
                updateBrightness(config.brightness)
                updateIllumination(config.illumination)
            }
        }
    }

    private fun registerListeners() {
        subscribeBluetoothStateListener(bluetoothStateListener)
        subscribeConnectionStateListener(connectionStateListener)
    }

    private fun disposeAll() {
        messageWriterDisposable?.dispose()
        messagePublisherDisposable?.dispose()
        btStateDisposable?.dispose()
        connectionStateDisposable?.dispose()
        configurationListenerDisposable?.dispose()
    }
}