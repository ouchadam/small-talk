package app.dapk.st.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.dapk.st.core.*
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.messenger.gallery.GetImageFromGallery
import app.dapk.st.messenger.state.ComposerStateChange
import app.dapk.st.messenger.state.MessengerState
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.state.state
import coil.request.ImageRequest
import kotlinx.parcelize.Parcelize

val LocalImageRequestFactory = staticCompositionLocalOf<ImageRequest.Builder> { throw IllegalAccessError() }

class MessengerActivity : DapkActivity() {

    private val module by unsafeLazy { module<MessengerModule>() }
    private val state: MessengerState by state { module.messengerState(readPayload()) }

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
        val factory = ImageRequest.Builder(applicationContext).fetcherFactory(module.decryptingFetcherFactory(RoomId(payload.roomId)))

        module.deviceMeta.onAtLeastR { setLocusContext(LocusId(payload.roomId), savedInstanceState) }

        val galleryLauncher = registerForActivityResult(GetImageFromGallery()) {
            it?.let { uri ->
                state.dispatch(
                    ComposerStateChange.SelectAttachmentToSend(
                        MessageAttachment(
                            AndroidUri(it.toString()),
                            MimeType.Image,
                        )
                    )
                )
            }
        }

        setContent {
            Surface(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalImageRequestFactory provides factory) {
                    MessengerScreen(state, navigator, galleryLauncher)
                }
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
