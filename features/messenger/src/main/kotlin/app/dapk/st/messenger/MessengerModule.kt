package app.dapk.st.messenger

import android.content.Context
import app.dapk.st.core.Base64
import app.dapk.st.core.ProvidableModule
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.matrix.common.RoomId

class MessengerModule(
    private val chatEngine: ChatEngine,
    private val context: Context,
    private val base64: Base64,
    private val messageOptionsStore: MessageOptionsStore,
) : ProvidableModule {

    internal fun messengerViewModel(): MessengerViewModel {
        return MessengerViewModel(
            chatEngine,
            messageOptionsStore,
        )
    }

    internal fun decryptingFetcherFactory(roomId: RoomId) = DecryptingFetcherFactory(context, base64, roomId)
}