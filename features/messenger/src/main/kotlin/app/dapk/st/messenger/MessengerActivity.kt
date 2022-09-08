package app.dapk.st.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.*
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.navigator.MessageAttachment
import kotlinx.parcelize.Parcelize

class MessengerActivity : DapkActivity() {

    private val viewModel by viewModel { module<MessengerModule>().messengerViewModel() }

    companion object {

        fun newInstance(context: Context, roomId: RoomId): Intent {
            return Intent(context, MessengerActivity::class.java).apply {
                putExtra("key", MessagerActivityPayload(roomId.value))
            }
        }

        fun newShortcutInstance(context: Context, roomId: RoomId): Intent {
            return Intent(context, MessengerActivity::class.java).apply {
                action = "from_shortcut"
                putExtra("shortcut_key", roomId.value)
            }
        }

        fun newMessageAttachment(context: Context, roomId: RoomId, attachments: List<MessageAttachment>): Intent {
            return Intent(context, MessengerActivity::class.java).apply {
                putExtra("key", MessagerActivityPayload(roomId.value, attachments))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = readPayload<MessagerActivityPayload>()
        log(AppLogTag.ERROR_NON_FATAL, payload)
        setContent {
                Surface(Modifier.fillMaxSize()) {
                    MessengerScreen(RoomId(payload.roomId), payload.attachments, viewModel, navigator)
                }
        }
    }
}

@Parcelize
data class MessagerActivityPayload(
    val roomId: String,
    val attachments: List<MessageAttachment>? = null
) : Parcelable

fun <T : Parcelable> Activity.readPayload(): T = intent.getParcelableExtra("key") ?: intent.getStringExtra("shortcut_key")!!.let {
    MessagerActivityPayload(it) as T
}