package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.core.extensions.ifNotEmpty
import app.dapk.st.matrix.sync.RoomMembersService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.matrix.sync.internal.request.ApiEphemeralEvent
import kotlinx.coroutines.flow.MutableSharedFlow

internal class EphemeralEventsUseCase(
    private val roomMembersService: RoomMembersService,
    private val syncEventsFlow: MutableSharedFlow<List<SyncService.SyncEvent>>,
) {

    suspend fun processEvents(roomToProcess: RoomToProcess) {
        val syncEvents = roomToProcess.apiSyncRoom.ephemeral?.events?.filterIsInstance<ApiEphemeralEvent.Typing>()?.map {
            val members = it.content.userIds.ifNotEmpty { roomMembersService.find(roomToProcess.roomId, it) }
            SyncService.SyncEvent.Typing(roomToProcess.roomId, members)
        }
        syncEvents?.let { syncEventsFlow.tryEmit(it) }
    }

}