package app.dapk.st.share

import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService

class ShareEntryModule(
    private val syncService: SyncService,
    private val roomService: RoomService,
) : ProvidableModule {

    fun shareEntryViewModel(): ShareEntryViewModel {
        return ShareEntryViewModel(FetchRoomsUseCase(syncService, roomService))
    }
}