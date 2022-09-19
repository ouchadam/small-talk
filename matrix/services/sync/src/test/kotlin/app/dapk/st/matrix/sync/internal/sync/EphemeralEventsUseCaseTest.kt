package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.matrix.sync.internal.request.ApiEphemeral
import fake.FakeRoomMembersService
import fixture.aRoomId
import fixture.aRoomMember
import fixture.aUserCredentials
import internalfixture.anApiEphemeral
import internalfixture.anApiSyncRoom
import internalfixture.anEphemeralTypingEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.TestSharedFlow

private val A_ROOM_ID = aRoomId()
private val A_ROOM_MEMBER = aRoomMember()

internal class EphemeralEventsUseCaseTest {

    private val fakeRoomMembersService = FakeRoomMembersService()
    private val testFlow = TestSharedFlow<List<SyncService.SyncEvent>>()

    private val ephemeralEventsUseCase = EphemeralEventsUseCase(fakeRoomMembersService, testFlow)

    @Test
    fun `given no ephemeral events to process then does nothing`() = runTest {
        val roomToProcess = aRoomToProcess()

        ephemeralEventsUseCase.processEvents(roomToProcess)

        testFlow.assertNoValues()
    }

    @Test
    fun `given known member is typing then emits typing`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_ROOM_MEMBER.id, A_ROOM_MEMBER)
        val roomToProcess = aRoomToProcess(anApiEphemeral(listOf(anEphemeralTypingEvent(listOf(A_ROOM_MEMBER.id)))))

        ephemeralEventsUseCase.processEvents(roomToProcess)

        testFlow.assertValues(
            listOf(SyncService.SyncEvent.Typing(A_ROOM_ID, listOf(A_ROOM_MEMBER)))
        )
    }

    @Test
    fun `given unknown member is typing then emits empty`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_ROOM_MEMBER.id, null)
        val roomToProcess = aRoomToProcess(anApiEphemeral(listOf(anEphemeralTypingEvent(listOf(A_ROOM_MEMBER.id)))))

        ephemeralEventsUseCase.processEvents(roomToProcess)

        testFlow.assertValues(listOf(SyncService.SyncEvent.Typing(A_ROOM_ID, emptyList())))
    }

    @Test
    fun `given member stops typing then emits empty`() = runTest {
        fakeRoomMembersService.givenNoMembers(A_ROOM_ID)
        val roomToProcess = aRoomToProcess(anApiEphemeral(listOf(anEphemeralTypingEvent(userIds = emptyList()))))

        ephemeralEventsUseCase.processEvents(roomToProcess)

        testFlow.assertValues(listOf(SyncService.SyncEvent.Typing(A_ROOM_ID, emptyList())))
    }
}

private fun aRoomToProcess(ephemeral: ApiEphemeral? = null) = RoomToProcess(
    A_ROOM_ID,
    anApiSyncRoom(ephemeral = ephemeral),
    directMessage = null,
    userCredentials = aUserCredentials(),
    heroes = null,
)
