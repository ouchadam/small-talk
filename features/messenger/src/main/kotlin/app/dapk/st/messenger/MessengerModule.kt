package app.dapk.st.messenger

import android.content.ClipboardManager
import android.content.Context
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.matrix.common.RoomId

class MessengerModule(
    private val chatEngine: ChatEngine,
    private val context: Context,
    private val messageOptionsStore: MessageOptionsStore,
    private val deviceMeta: DeviceMeta,
) : ProvidableModule {

    internal fun messengerViewModel(): MessengerViewModel {
        return MessengerViewModel(
            chatEngine,
            messageOptionsStore,
            CopyToClipboard(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager),
            deviceMeta,
        )
    }

    internal fun decryptingFetcherFactory(roomId: RoomId) = DecryptingFetcherFactory(context, roomId, chatEngine.mediaDecrypter())
}