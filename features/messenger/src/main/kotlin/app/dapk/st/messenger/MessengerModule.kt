package app.dapk.st.messenger

import android.content.Context
import app.dapk.st.core.Base64
import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService
import java.time.Clock

class MessengerModule(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
    private val clock: Clock,
    private val context: Context,
    private val base64: Base64,
    private val imageMetaReader: ImageContentReader,
) : ProvidableModule {

    internal fun messengerViewModel(): MessengerViewModel {
        return MessengerViewModel(messageService, roomService, roomStore, credentialsStore, timelineUseCase(), LocalIdFactory(), imageMetaReader, clock)
    }

    private fun timelineUseCase(): TimelineUseCaseImpl {
        val mergeWithLocalEchosUseCase = MergeWithLocalEchosUseCaseImpl(LocalEchoMapper(MetaMapper()))
        return TimelineUseCaseImpl(syncService, messageService, roomService, mergeWithLocalEchosUseCase)
    }

    internal fun decryptingFetcherFactory(roomId: RoomId) = DecryptingFetcherFactory(context, base64, roomId)
}