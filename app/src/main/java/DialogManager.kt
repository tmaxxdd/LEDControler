import android.content.Context
import com.czterysery.ledcontroller.R
import com.rey.material.app.Dialog
import com.rey.material.app.SimpleDialog

class DialogManager(context: Context) {

    val loading: Dialog = Dialog(context, R.style.CustomDialog)
            .title(context.getString(R.string.connecting_title))
            .contentView(R.layout.dialog_loading_view)
            .cancelable(false)
    
    val btNotSupported: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.bt_not_supported_message)
            .title(R.string.bt_not_supported_title)
            .positiveAction(R.string.close_app)
            .cancelable(false)

    val enableBT: Dialog = SimpleDialog(context, R.style.CustomDialog)
            .message(R.string.enable_bt_message)
            .title(R.string.bluetooth_disabled)
            .positiveAction(R.string.turn_on)
            .negativeAction(R.string.cancel)
            .cancelable(true)

    fun hideAll() {
        btNotSupported.dismiss()
        loading.dismiss()
    }

    fun dismissAll() {
        btNotSupported.dismissImmediately()
        enableBT.dismissImmediately()
        loading.dismissImmediately()
    }
}