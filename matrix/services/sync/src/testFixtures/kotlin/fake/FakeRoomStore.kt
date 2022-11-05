package fake

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import test.delegateReturn

class FakeRoomStore : RoomStore by mockk() {

    fun verifyNoUnreadChanges() {
        coVerify(exactly = 0) { insertUnread(RoomId(any()), any()) }
        coVerify(exactly = 0) { markRead(RoomId(any())) }
    }

    fun verifyRoomMarkedRead(roomId: RoomId) {
        coVerify { markRead(roomId) }
    }

    fun verifyInsertsEvents(roomId: RoomId, events: List<EventId>) {
        coVerify { insertUnread(roomId, events) }
    }

    fun givenEvent(eventId: EventId, result: RoomEvent?) {
        coEvery { findEvent(eventId) } returns result
    }

    fun givenUnreadEvents(unreadEvents: Flow<Map<RoomOverview, List<RoomEvent>>>) {
        every { observeUnread() } returns unreadEvents
    }

    fun givenUnreadEvents() = every { observeUnread() }.delegateReturn()
    fun givenUnreadByCount() = every { observeUnreadCountById() }.delegateReturn()

    fun givenNotMutedUnreadEvents(unreadEvents: Flow<Map<RoomOverview, List<RoomEvent>>>) {
        every { observeNotMutedUnread() } returns unreadEvents
    }

    fun givenMuted() = every { observeMuted() }.delegateReturn()

}