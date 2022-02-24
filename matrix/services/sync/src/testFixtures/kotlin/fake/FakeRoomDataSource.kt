package fake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.internal.sync.RoomDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class FakeRoomDataSource {
    val instance = mockk<RoomDataSource>()

    fun givenNoCachedRoom(roomId: RoomId) {
        coEvery { instance.read(roomId) } returns null
    }

    fun givenRoom(roomId: RoomId, roomState: RoomState?) {
        coEvery { instance.read(roomId) } returns roomState
    }

    fun verifyNoChanges() {
        coVerify(exactly = 0) { instance.persist(RoomId(any()), any(), any()) }
    }

    fun verifyRoomUpdated(previousEvents: RoomState?, newState: RoomState) {
        coVerify { instance.persist(newState.roomOverview.roomId, previousEvents, newState) }
    }
}