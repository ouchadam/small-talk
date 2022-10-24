package app.dapk.st.messenger

import android.content.ClipData
import android.content.ClipboardManager

class CopyToClipboard(private val clipboard: ClipboardManager) {

    fun copy(copyable: Copyable) {

        clipboard.addPrimaryClipChangedListener { }

        when (copyable) {
            is Copyable.Text -> {
                clipboard.setPrimaryClip(ClipData.newPlainText("", copyable.value))
            }
        }
    }

    sealed interface Copyable {
        data class Text(val value: String) : Copyable
    }
}
