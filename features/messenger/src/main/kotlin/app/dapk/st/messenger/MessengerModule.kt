package app.dapk.st.messenger

import android.content.ClipboardManager
import android.content.Context
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.state.createStateViewModel
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.messenger.state.MessengerState
import app.dapk.st.messenger.state.messengerReducer

class MessengerModule(
    private val chatEngine: ChatEngine,
    private val context: Context,
    private val messageOptionsStore: MessageOptionsStore,
    val deviceMeta: DeviceMeta,
) : ProvidableModule {

    internal fun messengerState(launchPayload: MessagerActivityPayload): MessengerState {
        return createStateViewModel {
            messengerReducer(
                JobBag(),
                chatEngine,
                CopyToClipboard(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager),
                deviceMeta,
                messageOptionsStore,
                RoomId(launchPayload.roomId),
                launchPayload.attachments,
                it
            )
        }
    }

    internal fun decryptingFetcherFactory(roomId: RoomId) = DecryptingFetcherFactory(context, roomId, chatEngine.mediaDecrypter())
}