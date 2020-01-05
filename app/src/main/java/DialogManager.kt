import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.czterysery.ledcontroller.R
import com.rey.material.app.Dialog
import kotlinx.android.synthetic.main.dialog_text_view.view.*

class DialogManager(private val activity: Activity) {
    private val textView = activity.layoutInflater.inflate(R.layout.dialog_text_view, null)

    val loading: Dialog = Dialog(activity)
            .title(activity.getString(R.string.connecting_title))
            .contentView(R.layout.dialog_loading_view)
            .cancelable(false)
    
    val btNotSupported: Dialog = Dialog(activity)
            .title(activity.getString(R.string.bt_not_supported_title))
            .contentView(getViewWithText(R.string.bt_not_supported_message))
            .positiveAction(R.string.close_app)
            .cancelable(false)


    val enableBT: Dialog = Dialog(activity)
            .title("Bluetooth disabled")
            .contentView(getViewWithText(R.string.enable_bt_message))
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

    private fun getViewWithText(messageId: Int): View {
        textView.dialogMessage.text = activity.getString(messageId)
        detachFromParentIfPresent(textView)
        return textView
    }

    private fun detachFromParentIfPresent(view: View) {
        view.parent?.let {
            (it as ViewGroup).removeView(view)
        }
    }

}