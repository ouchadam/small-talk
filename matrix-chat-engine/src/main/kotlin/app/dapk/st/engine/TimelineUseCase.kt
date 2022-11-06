package app.dapk.st.engine

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal typealias ObserveTimelineUseCase = (RoomId, UserId) -> Flow<MessengerPageState>

internal class TimelineUseCaseImpl(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val timelineMergeWithLocalEchosUseCase: TimelineMergeWithLocalEchosUseCase,
) : ObserveTimelineUseCase {

    override fun invoke(roomId: RoomId, userId: UserId): Flow<MessengerPageState> {
        return combine(
            roomDatasource(roomId),
            messageService.localEchos(roomId),
            syncService.events(roomId),
            roomService.observeIsMuted(roomId),
        ) { roomState, localEchos, events, isMuted ->
            MessengerPageState(
                roomState = when {
                    localEchos.isEmpty() -> roomState
                    else -> {
                        timelineMergeWithLocalEchosUseCase.invoke(
                            roomState,
                            roomService.findMember(roomId, userId) ?: userId.toFallbackMember(),
                            localEchos,
                        )
                    }
                },
                typing = events.filterIsInstance<SyncService.SyncEvent.Typing>().firstOrNull { it.roomId == roomId }?.engine(),
                self = userId,
                isMuted = isMuted,
            )
        }
    }

    private fun roomDatasource(roomId: RoomId) = combine(
        syncService.startSyncing(),
        syncService.room(roomId).map { it.engine() }
    ) { _, room -> room }
}

private fun UserId.toFallbackMember() = RoomMember(this, displayName = null, avatarUrl = null)
