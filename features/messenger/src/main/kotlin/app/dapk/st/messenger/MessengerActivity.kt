package app.dapk.st.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.matrix.common.RoomId
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = readPayload<MessagerActivityPayload>()
        setContent {
            SmallTalkTheme {
                Surface(Modifier.fillMaxSize()) {
                    MessengerScreen(RoomId(payload.roomId), viewModel, navigator)
                }
            }
        }
    }
}

@Parcelize
data class MessagerActivityPayload(
    val roomId: String
) : Parcelable

fun <T : Parcelable> Activity.readPayload(): T = intent.getParcelableExtra("key")!!