package app.dapk.st.engine

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

class ReadMarkingTimeline(
    private val roomStore: RoomStore,
    private val credentialsStore: CredentialsStore,
    private val observeTimelineUseCase: ObserveTimelineUseCase,
    private val roomService: RoomService,
) {

    suspend fun foo(roomId: RoomId, isReadReceiptsDisabled: Boolean): Flow<MessengerState> {
        var lastKnownReadEvent: EventId? = null
        val credentials = credentialsStore.credentials()!!
        roomStore.markRead(roomId)
        return observeTimelineUseCase.invoke(roomId, credentials.userId).distinctUntilChanged().onEach { state ->
            state.latestMessageEventFromOthers(self = credentials.userId)?.let {
                if (lastKnownReadEvent != it) {
                    updateRoomReadStateAsync(latestReadEvent = it, state, isReadReceiptsDisabled)
                    lastKnownReadEvent = it
                }
            }
        }
    }

    private suspend fun updateRoomReadStateAsync(latestReadEvent: EventId, state: MessengerState, isReadReceiptsDisabled: Boolean): Deferred<*> {
        return coroutineScope {
            async {
                runCatching {
                    roomService.markFullyRead(state.roomState.roomOverview.roomId, latestReadEvent, isPrivate = isReadReceiptsDisabled)
                    roomStore.markRead(state.roomState.roomOverview.roomId)
                }
            }
        }
    }

}

private fun MessengerState.latestMessageEventFromOthers(self: UserId) = this.roomState.events
    .filterIsInstance<RoomEvent.Message>()
    .filterNot { it.author.id == self }
    .firstOrNull()
    ?.eventId
