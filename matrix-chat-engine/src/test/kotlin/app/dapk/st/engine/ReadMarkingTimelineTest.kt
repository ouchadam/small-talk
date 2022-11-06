package app.dapk.st.engine

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import fake.FakeCredentialsStore
import fake.FakeRoomStore
import fixture.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private val A_ROOM_ID = aRoomId()
private val A_USER_CREDENTIALS = aUserCredentials()
private val A_ROOM_MESSAGE_FROM_OTHER_USER = aRoomMessageEvent(author = aRoomMember(id = aUserId("another-user")))
private val A_ROOM_MESSAGE_FROM_SELF = aRoomMessageEvent(author = aRoomMember(id = A_USER_CREDENTIALS.userId))
private val READ_RECEIPTS_ARE_DISABLED = true

class ReadMarkingTimelineTest {

    private val fakeRoomStore = FakeRoomStore()
    private val fakeCredentialsStore = FakeCredentialsStore().apply { givenCredentials().returns(A_USER_CREDENTIALS) }
    private val fakeObserveTimelineUseCase = FakeObserveTimelineUseCase()
    private val fakeRoomService = FakeRoomService()

    private val readMarkingTimeline = ReadMarkingTimeline(
        fakeRoomStore,
        fakeCredentialsStore,
        fakeObserveTimelineUseCase,
        fakeRoomService,
    )

    @Test
    fun `given a message from self, when fetching, then only marks room as read on initial launch`() = runExpectTest {
        fakeRoomStore.expectUnit(times = 1) { it.markRead(A_ROOM_ID) }
        val messengerState = aMessengerState(roomState = aRoomState(events = listOf(A_ROOM_MESSAGE_FROM_SELF)))
        fakeObserveTimelineUseCase.given(A_ROOM_ID, A_USER_CREDENTIALS.userId).returns(flowOf(messengerState))

        val result = readMarkingTimeline.fetch(A_ROOM_ID, isReadReceiptsDisabled = READ_RECEIPTS_ARE_DISABLED).first()

        result shouldBeEqualTo messengerState
        verifyExpects()
    }

    @Test
    fun `given a message from other user, when fetching, then marks room as read`() = runExpectTest {
        fakeRoomStore.expectUnit(times = 2) { it.markRead(A_ROOM_ID) }
        fakeRoomService.expectUnit { it.markFullyRead(A_ROOM_ID, A_ROOM_MESSAGE_FROM_OTHER_USER.eventId, isPrivate = READ_RECEIPTS_ARE_DISABLED) }
        val messengerState = aMessengerState(roomState = aRoomState(events = listOf(A_ROOM_MESSAGE_FROM_OTHER_USER)))
        fakeObserveTimelineUseCase.given(A_ROOM_ID, A_USER_CREDENTIALS.userId).returns(flowOf(messengerState))

        val result = readMarkingTimeline.fetch(A_ROOM_ID, isReadReceiptsDisabled = READ_RECEIPTS_ARE_DISABLED).first()

        result shouldBeEqualTo messengerState
        verifyExpects()
    }

}

class FakeObserveTimelineUseCase : ObserveTimelineUseCase by mockk() {
    fun given(roomId: RoomId, userId: UserId) = every { this@FakeObserveTimelineUseCase.invoke(roomId, userId) }.delegateReturn()
}
