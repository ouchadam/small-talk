package app.dapk.st.messenger

import ViewModelTest
import app.dapk.st.core.Lce
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.SyncService
import fake.FakeCredentialsStore
import fake.FakeRoomStore
import fixture.*
import internalfake.FakeLocalIdFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.delegateReturn
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val A_CURRENT_TIMESTAMP = 10000L
private val A_ROOM_ID = aRoomId("messenger state room id")
private const val A_MESSAGE_CONTENT = "message content"
private const val A_LOCAL_ID = "local.1111-2222-3333"
private val AN_EVENT_ID = anEventId("state event")
private val A_SELF_ID = aUserId("self")

class MessengerViewModelTest {

    private val runViewModelTest = ViewModelTest()

    private val fakeMessageService = FakeMessageService()
    private val fakeRoomService = FakeRoomService()
    private val fakeRoomStore = FakeRoomStore()
    private val fakeCredentialsStore = FakeCredentialsStore().also { it.givenCredentials().returns(aUserCredentials(userId = A_SELF_ID)) }
    private val fakeObserveTimelineUseCase = FakeObserveTimelineUseCase()

    private val viewModel = MessengerViewModel(
        fakeMessageService,
        fakeRoomService,
        fakeRoomStore,
        fakeCredentialsStore,
        fakeObserveTimelineUseCase,
        localIdFactory = FakeLocalIdFactory().also { it.givenCreate().returns(A_LOCAL_ID) }.instance,
        imageContentReader = FakeImageContentReader(),
        clock = fixedClock(A_CURRENT_TIMESTAMP),
        factory = runViewModelTest.testMutableStateFactory(),
    )

    @Test
    fun `when creating view model, then initial state is loading room state`() = runViewModelTest {
        viewModel.test()

        assertInitialState(
            MessengerScreenState(
                roomId = null,
                roomState = Lce.Loading(),
                composerState = ComposerState.Text(value = "", reply = null)
            )
        )
    }

    @Test
    fun `given timeline emits state, when starting, then updates state and marks room and events as read`() = runViewModelTest {
        fakeRoomStore.expectUnit(times = 2) { it.markRead(A_ROOM_ID) }
        fakeRoomService.expectUnit { it.markFullyRead(A_ROOM_ID, AN_EVENT_ID) }
        val state = aMessengerStateWithEvent(AN_EVENT_ID, A_SELF_ID)
        fakeObserveTimelineUseCase.given(A_ROOM_ID, A_SELF_ID).returns(flowOf(state))

        viewModel.test().post(MessengerAction.OnMessengerVisible(A_ROOM_ID, attachments = null))

        assertStates<MessengerScreenState>(
            { copy(roomId = A_ROOM_ID) },
            { copy(roomState = Lce.Content(state)) },
        )
        verifyExpects()
    }

    @Test
    fun `when posting composer update, then updates state`() = runViewModelTest {
        viewModel.test().post(MessengerAction.ComposerTextUpdate(A_MESSAGE_CONTENT))

        assertStates<MessengerScreenState>({
            copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null))
        })
    }

    @Test
    fun `given composer message state when posting send text, then resets composer state and sends message`() = runViewModelTest {
        fakeMessageService.expectUnit { it.scheduleMessage(expectEncryptedMessage(A_ROOM_ID, A_LOCAL_ID, A_CURRENT_TIMESTAMP, A_MESSAGE_CONTENT)) }

        viewModel.test(initialState = initialStateWithComposerMessage(A_ROOM_ID, A_MESSAGE_CONTENT)).post(MessengerAction.ComposerSendText)

        assertStates<MessengerScreenState>({ copy(composerState = ComposerState.Text("", reply = null)) })
        verifyExpects()
    }

    private fun initialStateWithComposerMessage(roomId: RoomId, messageContent: String): MessengerScreenState {
        val roomState = RoomState(
            aRoomOverview(roomId = roomId, isEncrypted = true),
            listOf(anEncryptedRoomMessageEvent(utcTimestamp = 1))
        )
        return aMessageScreenState(roomId, aMessengerState(roomState = roomState), messageContent)
    }

    private fun expectEncryptedMessage(roomId: RoomId, localId: String, timestamp: Long, messageContent: String): MessageService.Message.TextMessage {
        val content = MessageService.Message.Content.TextContent(body = messageContent)
        return MessageService.Message.TextMessage(content, sendEncrypted = true, roomId, localId, timestamp)
    }

    private fun aMessengerStateWithEvent(eventId: EventId, selfId: UserId) = aRoomStateWithEventId(eventId).toMessengerState(selfId)

    private fun RoomState.toMessengerState(selfId: UserId) = aMessengerState(self = selfId, roomState = this)

    private fun aRoomStateWithEventId(eventId: EventId): RoomState {
        val element = anEncryptedRoomMessageEvent(eventId = eventId, utcTimestamp = 1)
        return RoomState(aRoomOverview(roomId = A_ROOM_ID, isEncrypted = true), listOf(element))
    }

}

fun aMessageScreenState(roomId: RoomId = aRoomId(), roomState: MessengerState, messageContent: String?) = MessengerScreenState(
    roomId = roomId,
    roomState = Lce.Content(roomState),
    composerState = ComposerState.Text(value = messageContent ?: "", reply = null)
)

fun aMessengerState(
    self: UserId = aUserId(),
    roomState: RoomState,
    typing: SyncService.SyncEvent.Typing? = null
) = MessengerState(self, roomState, typing)

class FakeObserveTimelineUseCase : ObserveTimelineUseCase by mockk() {
    fun given(roomId: RoomId, selfId: UserId) = coEvery { this@FakeObserveTimelineUseCase.invoke(roomId, selfId) }.delegateReturn()
}

class FakeMessageService : MessageService by mockk() {

    fun givenEchos(roomId: RoomId) = every { localEchos(roomId) }.delegateReturn()

}

class FakeRoomService : RoomService by mockk() {
    fun givenFindMember(roomId: RoomId, userId: UserId) = coEvery { findMember(roomId, userId) }.delegateReturn()
}

fun fixedClock(timestamp: Long = 0) = Clock.fixed(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)

class FakeImageContentReader: ImageContentReader by mockk()