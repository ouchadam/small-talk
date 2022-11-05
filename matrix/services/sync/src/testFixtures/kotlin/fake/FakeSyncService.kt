package fake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.SyncService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import test.delegateReturn

class FakeSyncService : SyncService by mockk() {
    fun givenStartsSyncing() = every { startSyncing() }.returns(flowOf(Unit))
    fun givenRoom(roomId: RoomId) = every { room(roomId) }.delegateReturn()
    fun givenEvents(roomId: RoomId? = null) = every { events(roomId) }.delegateReturn()
    fun givenInvites() = every { invites() }.delegateReturn()
    fun givenOverview() = every { overview() }.delegateReturn()
}
