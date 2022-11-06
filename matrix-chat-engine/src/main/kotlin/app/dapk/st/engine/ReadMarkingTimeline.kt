package app.dapk.st.engine

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

class ReadMarkingTimeline(
    private val roomStore: RoomStore,
    private val credentialsStore: CredentialsStore,
    private val observeTimelineUseCase: ObserveTimelineUseCase,
    private val roomService: RoomService,
) {

    fun fetch(roomId: RoomId, isReadReceiptsDisabled: Boolean): Flow<MessengerPageState> {
        return flow {
            val credentials = credentialsStore.credentials()!!
            roomStore.markRead(roomId)
            emit(credentials)
        }.flatMapConcat { credentials ->
            var lastKnownReadEvent: EventId? = null
            observeTimelineUseCase.invoke(roomId, credentials.userId).distinctUntilChanged().onEach { state ->
                state.latestMessageEventFromOthers(self = credentials.userId)?.let {
                    if (lastKnownReadEvent != it) {
                        updateRoomReadStateAsync(latestReadEvent = it, state, isReadReceiptsDisabled)
                        lastKnownReadEvent = it
                    }
                }
            }
        }
    }

    @Suppress("DeferredResultUnused")
    private suspend fun updateRoomReadStateAsync(latestReadEvent: EventId, state: MessengerPageState, isReadReceiptsDisabled: Boolean) {
        coroutineScope {
            async {
                runCatching {
                    roomService.markFullyRead(state.roomState.roomOverview.roomId, latestReadEvent, isPrivate = isReadReceiptsDisabled)
                    roomStore.markRead(state.roomState.roomOverview.roomId)
                }
            }
        }
    }

    private fun MessengerPageState.latestMessageEventFromOthers(self: UserId) = this.roomState.events
        .filterIsInstance<RoomEvent.Message>()
        .filterNot { it.author.id == self }
        .firstOrNull()
        ?.eventId
}