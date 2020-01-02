package com.czterysery.ledcontroller

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.czterysery.ledcontroller.data.model.BluetoothState
import com.czterysery.ledcontroller.data.model.Disabled
import com.czterysery.ledcontroller.data.model.Enabled
import com.czterysery.ledcontroller.data.model.None
import com.czterysery.ledcontroller.data.model.NotSupported
import io.reactivex.rxjava3.subjects.BehaviorSubject

class BluetoothStateBroadcastReceiver : BroadcastReceiver() {

    val btState: BehaviorSubject<BluetoothState> = BehaviorSubject.createDefault(None)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED).let {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_ON -> btState.onNext(Enabled)
                    BluetoothAdapter.STATE_OFF -> btState.onNext(Disabled)
                    BluetoothAdapter.ERROR -> btState.onNext(NotSupported)
                }
            }
        } else {
            btState.onNext(None)
        }
    }
}