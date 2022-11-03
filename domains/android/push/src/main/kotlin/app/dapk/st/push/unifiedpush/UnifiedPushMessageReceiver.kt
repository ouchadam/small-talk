package app.dapk.st.push.unifiedpush

import android.content.Context
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import kotlinx.serialization.json.Json
import org.unifiedpush.android.connector.MessagingReceiver

class UnifiedPushMessageReceiver : MessagingReceiver() {

    private val delegate = UnifiedPushMessageDelegate()

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        delegate.onMessage(context, message)
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        delegate.onNewEndpoint(context, endpoint)
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        log(AppLogTag.PUSH, "UnifiedPush onRegistrationFailed")
    }

    override fun onUnregistered(context: Context, instance: String) {
        log(AppLogTag.PUSH, "UnifiedPush onUnregistered")
    }

}
