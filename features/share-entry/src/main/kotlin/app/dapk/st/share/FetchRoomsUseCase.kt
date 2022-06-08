package app.dapk.st.share

import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.first

class FetchRoomsUseCase(
    private val syncSyncService: SyncService,
    private val roomService: RoomService,
) {

    suspend fun bar(): List<Item> {
        return syncSyncService.overview().first().map {
            Item(
                it.roomId,
                it.roomAvatarUrl,
                it.roomName ?: "",
                roomService.findMembersSummary(it.roomId).map { it.displayName ?: it.id.value }
            )
        }
    }
}