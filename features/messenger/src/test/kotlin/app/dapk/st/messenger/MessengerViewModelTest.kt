package app.dapk.st.messenger

import ViewModelTest
import app.dapk.st.core.Lce
import app.dapk.st.engine.MessengerState
import app.dapk.st.engine.RoomState
import app.dapk.st.engine.SendMessage
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import fake.FakeChatEngine
import fake.FakeMessageOptionsStore
import fixture.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

private const val READ_RECEIPTS_ARE_DISABLED = true
private val A_ROOM_ID = aRoomId("messenger state room id")
private const val A_MESSAGE_CONTENT = "message content"
private val AN_EVENT_ID = anEventId("state event")
private val A_SELF_ID = aUserId("self")

class MessengerViewModelTest {

    private val runViewModelTest = ViewModelTest()

    private val fakeMessageOptionsStore = FakeMessageOptionsStore()
    private val fakeChatEngine = FakeChatEngine()

    private val viewModel = MessengerViewModel(
        fakeChatEngine,
        fakeMessageOptionsStore.instance,
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
        fakeMessageOptionsStore.givenReadReceiptsDisabled().returns(READ_RECEIPTS_ARE_DISABLED)
        val state = aMessengerStateWithEvent(AN_EVENT_ID, A_SELF_ID)
        fakeChatEngine.givenMessages(A_ROOM_ID, READ_RECEIPTS_ARE_DISABLED).returns(flowOf(state))

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
        fakeChatEngine.expectUnit { it.send(expectTextMessage(A_MESSAGE_CONTENT), aRoomOverview()) }

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

    private fun expectTextMessage(messageContent: String): SendMessage.TextMessage {
        return SendMessage.TextMessage(messageContent, reply = null)
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
