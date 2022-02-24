package fixture

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.crypto.RoomMembersProvider
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeRoomMembersProvider : RoomMembersProvider by mockk() {
    fun givenUserIdsForRoom(roomId: RoomId) = coEvery { userIdsForRoom(roomId) }.delegateReturn()
}