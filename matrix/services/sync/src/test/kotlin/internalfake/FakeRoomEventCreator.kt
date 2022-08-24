package internalfake

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.sync.LookupResult
import app.dapk.st.matrix.sync.internal.sync.RoomEventCreator
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking

internal class FakeRoomEventCreator {
    val instance = mockk<RoomEventCreator>()

    fun givenCreates(roomId: RoomId, event: ApiTimelineEvent.Encrypted, result: RoomEvent) {
        coEvery { with(instance) { event.toRoomEvent(roomId) } } returns result
    }

    fun givenCreatesUsingLookup(
        userCredentials: UserCredentials,
        roomId: RoomId,
        eventIdToLookup: EventId,
        event: ApiTimelineEvent.TimelineMessage,
        result: RoomEvent,
        lookupResult: LookupResult
    ) {
        val slot = slot<suspend (EventId) -> LookupResult>()
        coEvery { with(instance) { event.toRoomEvent(userCredentials, roomId, capture(slot)) } } answers {
            runBlocking {
                if (slot.captured.invoke(eventIdToLookup) == lookupResult) {
                    result
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }
}