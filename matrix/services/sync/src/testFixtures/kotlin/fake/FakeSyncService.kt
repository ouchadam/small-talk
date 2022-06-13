package fake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.SyncService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import test.delegateReturn

class FakeSyncService : SyncService by mockk() {
    fun givenStartsSyncing() {
        every { startSyncing() }.returns(emptyFlow())
    }

    fun givenRoom(roomId: RoomId) = every { room(roomId) }.delegateReturn()

    fun givenEvents(roomId: RoomId) = every { events() }.delegateReturn()

}
