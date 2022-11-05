package app.dapk.st.engine

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.sync.InviteMeta
import fake.FakeSyncService
import fixture.aRoomId
import fixture.aRoomMember
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import app.dapk.st.matrix.sync.RoomInvite as MatrixRoomInvite

class InviteUseCaseTest {

    private val fakeSyncService = FakeSyncService()
    private val useCase = InviteUseCase(fakeSyncService)

    @Test
    fun `reads invites from sync service and maps to engine`() = runTest {
        val aMatrixRoomInvite = aMatrixRoomInvite()
        fakeSyncService.givenStartsSyncing()
        fakeSyncService.givenInvites().returns(flowOf(listOf(aMatrixRoomInvite)))

        val result = useCase.invites().first()

        result shouldBeEqualTo listOf(aMatrixRoomInvite.engine())
    }

}

fun aMatrixRoomInvite(
    from: RoomMember = aRoomMember(),
    roomId: RoomId = aRoomId(),
    inviteMeta: InviteMeta = InviteMeta.DirectMessage,
) = MatrixRoomInvite(from, roomId, inviteMeta)

