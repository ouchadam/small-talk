package app.dapk.st.messenger

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.*

internal typealias ObserveTimelineUseCase = (RoomId, UserId) -> Flow<MessengerState>

internal class TimelineUseCaseImpl(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val mergeWithLocalEchosUseCase: MergeWithLocalEchosUseCase
) : ObserveTimelineUseCase {

    override fun invoke(roomId: RoomId, userId: UserId): Flow<MessengerState> {
        return combine(
            roomDatasource(roomId),
            messageService.localEchos(roomId),
            syncService.events(roomId)
        ) { roomState, localEchos, events ->
            MessengerState(
                roomState = when {
                    localEchos.isEmpty() -> roomState
                    else -> {
                        mergeWithLocalEchosUseCase.invoke(
                            roomState,
                            roomService.findMember(roomId, userId) ?: userId.toFallbackMember(),
                            localEchos,
                        )
                    }
                },
                typing = events.filterIsInstance<SyncService.SyncEvent.Typing>().firstOrNull { it.roomId == roomId },
                self = userId,
            )
        }
    }

    private fun roomDatasource(roomId: RoomId) = combine(
        syncService.startSyncing().map { false }.onStart { emit(true) }.filter { it },
        syncService.room(roomId)
    ) { _, room -> room }
}

private fun UserId.toFallbackMember() = RoomMember(this, displayName = null, avatarUrl = null)

data class MessengerState(
    val self: UserId,
    val roomState: RoomState,
    val typing: SyncService.SyncEvent.Typing?
)
