package app.dapk.st.engine

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.sync.RoomOverview
import fake.FakeCredentialsStore
import fake.FakeRoomStore
import fake.FakeSyncService
import fixture.aMatrixRoomOverview
import fixture.aRoomMember
import fixture.aTypingEvent
import fixture.aUserCredentials
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn

private val A_ROOM_OVERVIEW = aMatrixRoomOverview()
private const val AN_UNREAD_COUNT = 10
private const val MUTED_ROOM = true
private val TYPING_MEMBERS = listOf(aRoomMember())

class DirectoryUseCaseTest {

    private val fakeSyncService = FakeSyncService()
    private val fakeMessageService = FakeMessageService()
    private val fakeCredentialsStore = FakeCredentialsStore()
    private val fakeRoomStore = FakeRoomStore()
    private val fakeMergeLocalEchosUseCase = FakeDirectoryMergeWithLocalEchosUseCase()

    private val useCase = DirectoryUseCase(
        fakeSyncService,
        fakeMessageService,
        fakeCredentialsStore,
        fakeRoomStore,
        fakeMergeLocalEchosUseCase,
    )

    @Test
    fun `given empty values, then reads default directory state and maps to engine`() = runTest {
        givenEmitsDirectoryState(
            A_ROOM_OVERVIEW,
            unreadCount = null,
            isMuted = false,
        )

        val result = useCase.state().first()

        result shouldBeEqualTo listOf(
            DirectoryItem(
                A_ROOM_OVERVIEW.engine(),
                unreadCount = UnreadCount(0),
                typing = null,
                isMuted = false
            )
        )
    }

    @Test
    fun `given extra state, then reads directory state and maps to engine`() = runTest {
        givenEmitsDirectoryState(
            A_ROOM_OVERVIEW,
            unreadCount = AN_UNREAD_COUNT,
            isMuted = MUTED_ROOM,
            typing = TYPING_MEMBERS
        )

        val result = useCase.state().first()

        result shouldBeEqualTo listOf(
            DirectoryItem(
                A_ROOM_OVERVIEW.engine(),
                unreadCount = UnreadCount(AN_UNREAD_COUNT),
                typing = aTypingEvent(A_ROOM_OVERVIEW.roomId, TYPING_MEMBERS),
                isMuted = MUTED_ROOM
            )
        )
    }

    private fun givenEmitsDirectoryState(
        roomOverview: RoomOverview,
        unreadCount: Int? = null,
        isMuted: Boolean = false,
        typing: List<RoomMember> = emptyList(),
    ) {
        val userCredentials = aUserCredentials()
        fakeCredentialsStore.givenCredentials().returns(userCredentials)

        val matrixOverviewState = listOf(roomOverview)

        fakeSyncService.givenStartsSyncing()
        fakeSyncService.givenOverview().returns(flowOf(matrixOverviewState))
        fakeSyncService.givenEvents().returns(flowOf(if (typing.isEmpty()) emptyList() else listOf(aTypingSyncEvent(roomOverview.roomId, typing))))

        fakeMessageService.givenEchos().returns(flowOf(emptyMap()))
        fakeRoomStore.givenUnreadByCount().returns(flowOf(unreadCount?.let { mapOf(roomOverview.roomId to it) } ?: emptyMap()))
        fakeRoomStore.givenMuted().returns(flowOf(if (isMuted) setOf(roomOverview.roomId) else emptySet()))

        val mappedOverview = roomOverview.engine()
        val expectedOverviewState = listOf(mappedOverview)
        fakeMergeLocalEchosUseCase.givenMergedEchos(expectedOverviewState, userCredentials.userId, emptyMap()).returns(expectedOverviewState)
    }
}

class FakeDirectoryMergeWithLocalEchosUseCase : DirectoryMergeWithLocalEchosUseCase by mockk() {
    fun givenMergedEchos(overviewState: OverviewState, selfId: UserId, echos: Map<RoomId, List<MessageService.LocalEcho>>) = coEvery {
        this@FakeDirectoryMergeWithLocalEchosUseCase.invoke(overviewState, selfId, echos)
    }.delegateReturn()
}
