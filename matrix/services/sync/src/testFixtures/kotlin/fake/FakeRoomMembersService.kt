package fake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.RoomMembersService
import io.mockk.coEvery
import io.mockk.mockk

class FakeRoomMembersService : RoomMembersService by mockk() {

    fun givenMember(roomId: RoomId, userId: UserId, roomMember: RoomMember?) {
        coEvery { find(roomId, listOf(userId)) } returns (roomMember?.let { listOf(it) } ?: emptyList())
    }

    fun givenNoMembers(roomId: RoomId) {
        coEvery { find(roomId, emptyList()) } returns emptyList()
    }
}