package app.dapk.st.engine

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.SyncService
import fake.FakeSyncService
import fixture.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.FlowTestObserver
import test.delegateReturn

private const val IS_ROOM_MUTED = false
private val A_ROOM_ID = aRoomId()
private val AN_USER_ID = aUserId()
private val A_ROOM_STATE = aMatrixRoomState()
private val A_MERGED_ROOM_STATE = A_ROOM_STATE.copy(events = listOf(aMatrixRoomMessageEvent(content = RichText.of("a merged event"))))
private val A_LOCAL_ECHOS_LIST = listOf(aLocalEcho())
private val A_ROOM_MEMBER = aRoomMember()

class TimelineUseCaseTest {

    private val fakeSyncService = FakeSyncService()
    private val fakeMessageService = FakeMessageService()
    private val fakeRoomService = FakeRoomService()
    private val fakeMergeWithLocalEchosUseCase = FakeMergeWithLocalEchosUseCase()

    private val timelineUseCase = TimelineUseCaseImpl(
        fakeSyncService,
        fakeMessageService,
        fakeRoomService,
        fakeMergeWithLocalEchosUseCase,
    )

    @Test
    fun `when observing timeline, then emits sync emission`() = runTest {
        givenSyncEmission(roomState = A_ROOM_STATE)

        timelineUseCase.invoke(A_ROOM_ID, AN_USER_ID)
            .test(this)
            .assertValues(
                listOf(
                    aMessengerState(self = AN_USER_ID, roomState = A_ROOM_STATE.engine())
                )
            )
    }

    @Test
    fun `given local echos, when observing timeline, then merges room and local echos`() = runTest {
        givenSyncEmission(roomState = A_ROOM_STATE, echos = A_LOCAL_ECHOS_LIST)
        fakeRoomService.givenFindMember(A_ROOM_ID, AN_USER_ID).returns(A_ROOM_MEMBER)

        fakeMergeWithLocalEchosUseCase.givenMerging(A_ROOM_STATE, A_ROOM_MEMBER, A_LOCAL_ECHOS_LIST).returns(A_MERGED_ROOM_STATE.engine())


        timelineUseCase.invoke(A_ROOM_ID, AN_USER_ID)
            .test(this)
            .assertValues(
                listOf(
                    aMessengerState(self = AN_USER_ID, roomState = A_MERGED_ROOM_STATE.engine())
                )
            )
    }

    @Test
    fun `given sync events from current and other rooms, when observing timeline, then filters by current room`() = runTest {
        givenSyncEmission(
            events = listOf(
                aTypingSyncEvent(aRoomId("another room"), members = listOf(A_ROOM_MEMBER)),
                aTypingSyncEvent(A_ROOM_ID, members = listOf(A_ROOM_MEMBER)),
            )
        )

        timelineUseCase.invoke(A_ROOM_ID, AN_USER_ID)
            .test(this)
            .assertValues(
                listOf(
                    aMessengerState(
                        self = AN_USER_ID,
                        roomState = A_ROOM_STATE.engine(),
                        typing = aTypingSyncEvent(A_ROOM_ID, members = listOf(A_ROOM_MEMBER)).engine()
                    )
                )
            )
    }

    private fun givenSyncEmission(
        roomState: RoomState = A_ROOM_STATE,
        echos: List<MessageService.LocalEcho> = emptyList(),
        events: List<SyncService.SyncEvent> = emptyList()
    ) {
        fakeSyncService.givenStartsSyncing()
        fakeSyncService.givenRoom(A_ROOM_ID).returns(flowOf(roomState))
        fakeMessageService.givenEchos(A_ROOM_ID).returns(flowOf(echos))
        fakeSyncService.givenEvents(A_ROOM_ID).returns(flowOf(events))
        fakeRoomService.givenMuted(A_ROOM_ID).returns(flowOf(IS_ROOM_MUTED))
    }
}

suspend fun <T> Flow<T>.test(scope: CoroutineScope) = FlowTestObserver(scope, this).also {
    this.collect()
}

class FakeMergeWithLocalEchosUseCase : TimelineMergeWithLocalEchosUseCase by mockk() {
    fun givenMerging(roomState: RoomState, roomMember: RoomMember, echos: List<MessageService.LocalEcho>) = every {
        this@FakeMergeWithLocalEchosUseCase.invoke(roomState.engine(), roomMember, echos)
    }.delegateReturn()
}

fun aTypingSyncEvent(
    roomId: RoomId = aRoomId(),
    members: List<RoomMember> = listOf(aRoomMember())
) = SyncService.SyncEvent.Typing(roomId, members)

class FakeMessageService : MessageService by mockk() {
    fun givenEchos(roomId: RoomId) = every { localEchos(roomId) }.delegateReturn()
    fun givenEchos() = every { localEchos() }.delegateReturn()
}

class FakeRoomService : RoomService by mockk() {
    fun givenFindMember(roomId: RoomId, userId: UserId) = coEvery { findMember(roomId, userId) }.delegateReturn()
    fun givenMuted(roomId: RoomId) = every { observeIsMuted(roomId) }.delegateReturn()
}

fun aMessengerState(
    self: UserId = aUserId(),
    roomState: app.dapk.st.engine.RoomState = aRoomState(),
    typing: Typing? = null,
    isMuted: Boolean = IS_ROOM_MUTED,
) = MessengerPageState(self, roomState, typing, isMuted)