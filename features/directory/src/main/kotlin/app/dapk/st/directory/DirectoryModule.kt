package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService

class DirectoryModule(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val context: Context,
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
) : ProvidableModule {

    fun directoryViewModel(): DirectoryViewModel {
        return DirectoryViewModel(
            ShortcutHandler(context),
            DirectoryUseCase(
                syncService,
                messageService,
                roomService,
                credentialsStore,
                roomStore,
            )
        )
    }
}