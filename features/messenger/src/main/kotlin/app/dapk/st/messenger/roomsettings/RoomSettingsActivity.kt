package app.dapk.st.messenger.roomsettings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.matrix.common.RoomId
import kotlinx.parcelize.Parcelize

class RoomSettingsActivity : DapkActivity() {

    companion object {
        fun newInstance(context: Context, roomId: RoomId): Intent {
            return Intent(context, RoomSettingsActivity::class.java).apply {
                putExtra("key", RoomSettingsActivityPayload(roomId.value))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = readPayload<RoomSettingsActivityPayload>()
        setContent {
            Surface(Modifier.fillMaxSize()) {
//                    MessengerScreen(RoomId(payload.roomId), payload.attachments, viewModel, navigator)
            }
        }
    }
}

@Parcelize
data class RoomSettingsActivityPayload(
    val roomId: String
) : Parcelable

fun <T : Parcelable> Activity.readPayload(): T = intent.getParcelableExtra("key")!!