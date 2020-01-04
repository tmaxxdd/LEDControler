import android.content.Context
import com.czterysery.ledcontroller.R
import com.rey.material.app.Dialog

class DialogManager(context: Context) {

    val btNotSupported: Dialog = Dialog(context)
            .title(context.getString(R.string.bt_not_supported_title))
            .positiveAction(R.string.close_app)
            .contentView(R.layout.dialog_text_view)
            .cancelable(false)

    val loading: Dialog = Dialog(context)
            .title("")

    fun hideAll() {
        btNotSupported.dismiss()
        loading.dismiss()
    }

    fun dismissAll() {
        btNotSupported.dismissImmediately()
        loading.dismissImmediately()
    }

}