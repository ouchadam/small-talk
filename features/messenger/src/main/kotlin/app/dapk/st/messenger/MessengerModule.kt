package app.dapk.st.messenger

import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService

class MessengerModule(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
) : ProvidableModule {

    fun messengerViewModel(): MessengerViewModel {
        return MessengerViewModel(syncService, messageService, roomService, roomStore, credentialsStore)
    }
}