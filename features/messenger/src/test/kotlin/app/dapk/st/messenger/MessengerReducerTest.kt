package app.dapk.st.messenger

import android.os.Build
import app.dapk.st.core.*
import app.dapk.st.design.components.BubbleModel
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.RoomState
import app.dapk.st.engine.SendMessage
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.common.asString
import app.dapk.st.messenger.state.*
import app.dapk.st.navigator.MessageAttachment
import fake.FakeChatEngine
import fake.FakeJobBag
import fake.FakeMessageOptionsStore
import fixture.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.*

private const val READ_RECEIPTS_ARE_DISABLED = true
private val A_ROOM_ID = aRoomId("messenger state room id")
private const val A_MESSAGE_CONTENT = "message content"
private val AN_EVENT_ID = anEventId("state event")
private val A_SELF_ID = aUserId("self")
private val A_MESSENGER_PAGE_STATE = aMessengerStateWithEvent(AN_EVENT_ID, A_SELF_ID)
private val A_MESSAGE_ATTACHMENT = MessageAttachment(AndroidUri("a-uri"), MimeType.Image)
private val A_REPLY = aRoomMessageEvent()
private val AN_SUPPORTED_REPLY = aRoomReplyMessageEvent()
private val AN_IMAGE_BUBBLE = BubbleModel.Image(
    BubbleModel.Image.ImageContent(100, 200, "a-url"),
    mockk(),
    BubbleModel.Event("author-id", "author-name", edited = false, time = "10:27")
)
private val A_TEXT_BUBBLE = BubbleModel.Text(
    content = RichText(listOf(RichText.Part.Normal(A_MESSAGE_CONTENT))),
    BubbleModel.Event("author-id", "author-name", edited = false, time = "10:27")
)
private val A_DIALOG_STATE = DialogState.PositiveNegative(
    "a title",
    "a subtitle",
    positiveAction = ScreenAction.LeaveRoomConfirmation.Confirm,
    negativeAction = ScreenAction.LeaveRoomConfirmation.Deny,
)

class MessengerReducerTest {

    private val fakeMessageOptionsStore = FakeMessageOptionsStore()
    private val fakeChatEngine = FakeChatEngine()
    private val fakeCopyToClipboard = FakeCopyToClipboard()
    private val fakeDeviceMeta = FakeDeviceMeta()
    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { fakeEventSource ->
        messengerReducer(
            fakeJobBag.instance,
            fakeChatEngine,
            fakeCopyToClipboard.instance,
            fakeDeviceMeta.instance,
            fakeMessageOptionsStore.instance,
            A_ROOM_ID,
            emptyList(),
            fakeEventSource,
        )
    }

    @Test
    fun `given empty initial attachments, then initial state is loading with text composer`() = reducerWithInitialState(initialAttachments = emptyList()) {
        assertInitialState(
            MessengerScreenState(
                roomId = A_ROOM_ID,
                roomState = Lce.Loading(),
                composerState = ComposerState.Text(value = "", reply = null),
                viewerState = null,
                dialogState = null,
            )
        )
    }

    @Test
    fun `given null initial attachments, then initial state is loading with text composer`() = reducerWithInitialState(initialAttachments = null) {
        assertInitialState(
            MessengerScreenState(
                roomId = A_ROOM_ID,
                roomState = Lce.Loading(),
                composerState = ComposerState.Text(value = "", reply = null),
                viewerState = null,
                dialogState = null,
            )
        )
    }

    @Test
    fun `given initial attachments, then initial state is loading attachment composer`() = reducerWithInitialState(listOf(A_MESSAGE_ATTACHMENT)) {
        assertInitialState(
            MessengerScreenState(
                roomId = A_ROOM_ID,
                roomState = Lce.Loading(),
                composerState = ComposerState.Attachments(listOf(A_MESSAGE_ATTACHMENT), reply = null),
                viewerState = null,
                dialogState = null,
            )
        )
    }

    @Test
    fun `given messages emits state, when Visible, then dispatches content`() = runReducerTest {
        fakeJobBag.instance.expect { it.replace("messages", any()) }
        fakeMessageOptionsStore.givenReadReceiptsDisabled().returns(READ_RECEIPTS_ARE_DISABLED)
        val state = aMessengerStateWithEvent(AN_EVENT_ID, A_SELF_ID)
        fakeChatEngine.givenMessages(A_ROOM_ID, READ_RECEIPTS_ARE_DISABLED).returns(flowOf(state))

        reduce(ComponentLifecycle.Visible)

        assertOnlyDispatches(listOf(MessagesStateChange.Content(state)))
    }

    @Test
    fun `when Gone, then cancels sync job`() = runReducerTest {
        fakeJobBag.instance.expect { it.cancel("messages") }

        reduce(ComponentLifecycle.Gone)

        assertNoChanges()
    }

    @Test
    fun `when Content StateChange, then updates room state`() = runReducerTest {
        reduce(MessagesStateChange.Content(A_MESSENGER_PAGE_STATE))

        assertOnlyStateChange { previous ->
            previous.copy(roomState = Lce.Content(A_MESSENGER_PAGE_STATE))
        }
    }

    @Test
    fun `when SelectAttachmentToSend, then updates composer state`() = runReducerTest {
        reduce(ComposerStateChange.SelectAttachmentToSend(A_MESSAGE_ATTACHMENT))

        assertOnlyStateChange { previous ->
            previous.copy(composerState = ComposerState.Attachments(listOf(A_MESSAGE_ATTACHMENT), reply = null))
        }
    }

    @Test
    fun `when Show ImagePreview, then updates viewer state`() = runReducerTest {
        reduce(ComposerStateChange.ImagePreview.Show(AN_IMAGE_BUBBLE))

        assertOnlyStateChange { previous ->
            previous.copy(viewerState = ViewerState(AN_IMAGE_BUBBLE))
        }
    }

    @Test
    fun `when Hide ImagePreview, then updates viewer state`() = runReducerTest {
        reduce(ComposerStateChange.ImagePreview.Hide)

        assertOnlyStateChange { previous ->
            previous.copy(viewerState = null)
        }
    }

    @Test
    fun `when TextUpdate StateChange, then updates composer state`() = runReducerTest {
        reduce(ComposerStateChange.TextUpdate(A_MESSAGE_CONTENT))

        assertOnlyStateChange { previous ->
            previous.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null))
        }
    }

    @Test
    fun `when Clear ComposerStateChange, then clear composer state`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null)) }

        reduce(ComposerStateChange.Clear)

        assertOnlyStateChange { previous ->
            previous.copy(composerState = ComposerState.Text(value = "", reply = null))
        }
    }

    @Test
    fun `given message text composer, when Enter ReplyMode, then updates composer state with reply`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null)) }

        reduce(ComposerStateChange.ReplyMode.Enter(A_REPLY))

        assertOnlyStateChange { previous ->
            previous.copy(composerState = (previous.composerState as ComposerState.Text).copy(reply = A_REPLY))
        }
    }

    @Test
    fun `given text composer, when Enter ReplyMode with unsupported content, then does nothing`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null)) }

        reduce(ComposerStateChange.ReplyMode.Enter(AN_SUPPORTED_REPLY))

        assertNoChanges()
    }

    @Test
    fun `given text composer, when Exit ReplyMode, then updates composer state`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = A_REPLY)) }

        reduce(ComposerStateChange.ReplyMode.Exit)

        assertOnlyStateChange { previous ->
            previous.copy(composerState = (previous.composerState as ComposerState.Text).copy(reply = null))
        }
    }

    @Test
    fun `given attachment composer, when Enter ReplyMode, then updates composer state with reply`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Attachments(listOf(A_MESSAGE_ATTACHMENT), reply = null)) }

        reduce(ComposerStateChange.ReplyMode.Enter(A_REPLY))

        assertOnlyStateChange { previous ->
            previous.copy(composerState = (previous.composerState as ComposerState.Attachments).copy(reply = A_REPLY))
        }
    }

    @Test
    fun `given attachment composer, when Exit ReplyMode, then updates composer state`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Attachments(listOf(A_MESSAGE_ATTACHMENT), reply = A_REPLY)) }

        reduce(ComposerStateChange.ReplyMode.Exit)

        assertOnlyStateChange { previous ->
            previous.copy(composerState = (previous.composerState as ComposerState.Attachments).copy(reply = null))
        }
    }

    @Test
    fun `when LeaveRoom, then updates dialog state with leave room confirmation`() = runReducerTest {
        reduce(ScreenAction.LeaveRoom)

        assertOnlyDispatches(
            ScreenAction.UpdateDialogState(
                DialogState.PositiveNegative(
                    title = "Leave room",
                    subtitle = "Are you sure you want you leave the room? If the room is private you will need to be invited again to rejoin.",
                    negativeAction = ScreenAction.LeaveRoomConfirmation.Deny,
                    positiveAction = ScreenAction.LeaveRoomConfirmation.Confirm,
                )
            )
        )
    }

    @Test
    fun `when UpdateDialogState, then updates dialog state`() = runReducerTest {
        reduce(ScreenAction.UpdateDialogState(dialogState = A_DIALOG_STATE))

        assertOnlyStateChange { it.copy(dialogState = A_DIALOG_STATE) }
    }

    @Test
    fun `given can leave room, when LeaveConfirmation Confirm, then removes dialog and rejects room and emits OnLeftRoom`() = runReducerTest {
        fakeChatEngine.expect { it.rejectRoom(A_ROOM_ID) }

        reduce(ScreenAction.LeaveRoomConfirmation.Confirm)

        assertDispatches(ScreenAction.UpdateDialogState(dialogState = null))
        assertEvents(MessengerEvent.OnLeftRoom)
        assertNoStateChange()
    }

    @Test
    fun `given leave room fails, when LeaveConfirmation Confirm, then removes dialog and emits toast`() = runReducerTest {
        fakeChatEngine.expectError(error = RuntimeException("an error")) { fakeChatEngine.rejectRoom(A_ROOM_ID) }

        reduce(ScreenAction.LeaveRoomConfirmation.Confirm)

        assertDispatches(ScreenAction.UpdateDialogState(dialogState = null))
        assertEvents(MessengerEvent.Toast("Failed to leave room"))
        assertNoStateChange()
    }

    @Test
    fun `when LeaveConfirmation Deny, then removes dialog and does nothing`() = runReducerTest {
        reduce(ScreenAction.LeaveRoomConfirmation.Deny)

        assertDispatches(ScreenAction.UpdateDialogState(dialogState = null))
        assertNoEvents()
        assertNoStateChange()
    }

    @Test
    fun `when OpenGalleryPicker, then emits event`() = runReducerTest {
        reduce(ScreenAction.OpenGalleryPicker)

        assertOnlyEvents(listOf(MessengerEvent.SelectImageAttachment))
    }

    @Test
    fun `given android api is lower than S_v2 and has text content, when CopyToClipboard, then copies to system and toasts`() = runReducerTest {
        fakeDeviceMeta.givenApiVersion().returns(Build.VERSION_CODES.S)
        fakeCopyToClipboard.instance.expect { it.copy(CopyToClipboard.Copyable.Text(A_MESSAGE_CONTENT)) }

        reduce(ScreenAction.CopyToClipboard(A_TEXT_BUBBLE))

        assertEvents(listOf(MessengerEvent.Toast("Copied to clipboard")))
        assertNoDispatches()
        assertNoStateChange()
    }

    @Test
    fun `given android api is higher than S_v2 and has text content, when CopyToClipboard, then copies to system and does not toast`() = runReducerTest {
        fakeDeviceMeta.givenApiVersion().returns(Build.VERSION_CODES.TIRAMISU)
        fakeCopyToClipboard.instance.expect { it.copy(CopyToClipboard.Copyable.Text(A_MESSAGE_CONTENT)) }

        reduce(ScreenAction.CopyToClipboard(A_TEXT_BUBBLE))

        assertNoChanges()
    }

    @Test
    fun `given image content, when CopyToClipboard, then toasts nothing to copy`() = runReducerTest {
        reduce(ScreenAction.CopyToClipboard(AN_IMAGE_BUBBLE))

        assertEvents(listOf(MessengerEvent.Toast("Nothing to copy")))
        assertNoDispatches()
        assertNoStateChange()
    }

    @Test
    fun `given text composer, when SendMessage, then clear composer and sends text message`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = null), roomState = Lce.Content(A_MESSENGER_PAGE_STATE)) }
        fakeChatEngine.expectUnit { it.send(expectTextMessage(A_MESSAGE_CONTENT), A_MESSENGER_PAGE_STATE.roomState.roomOverview) }

        reduce(ScreenAction.SendMessage)

        assertDispatches(listOf(ComposerStateChange.Clear))
        assertNoEvents()
        assertNoStateChange()
    }

    @Test
    fun `given text composer with reply, when SendMessage, then clear composer and sends text message`() = runReducerTest {
        setState { it.copy(composerState = ComposerState.Text(A_MESSAGE_CONTENT, reply = A_REPLY), roomState = Lce.Content(A_MESSENGER_PAGE_STATE)) }
        fakeChatEngine.expectUnit { it.send(expectTextMessage(A_MESSAGE_CONTENT, reply = A_REPLY), A_MESSENGER_PAGE_STATE.roomState.roomOverview) }

        reduce(ScreenAction.SendMessage)

        assertDispatches(listOf(ComposerStateChange.Clear))
        assertNoEvents()
        assertNoStateChange()
    }

    @Test
    fun `given attachment composer, when SendMessage, then clear composer and sends image message`() = runReducerTest {
        setState {
            it.copy(
                composerState = ComposerState.Attachments(listOf(A_MESSAGE_ATTACHMENT), reply = null),
                roomState = Lce.Content(A_MESSENGER_PAGE_STATE)
            )
        }
        fakeChatEngine.expectUnit { it.send(expectImageMessage(A_MESSAGE_ATTACHMENT.uri), A_MESSENGER_PAGE_STATE.roomState.roomOverview) }

        reduce(ScreenAction.SendMessage)

        assertDispatches(listOf(ComposerStateChange.Clear))
        assertNoEvents()
        assertNoStateChange()
    }

    private fun expectTextMessage(messageContent: String, reply: RoomEvent? = null): SendMessage.TextMessage {
        return SendMessage.TextMessage(messageContent, reply = reply?.toSendMessageReply())
    }

    private fun expectImageMessage(uri: AndroidUri): SendMessage.ImageMessage {
        return SendMessage.ImageMessage(uri.value)
    }

    private fun RoomEvent.toSendMessageReply() = SendMessage.TextMessage.Reply(
        author = this.author,
        originalMessage = when (this) {
            is RoomEvent.Image -> TODO()
            is RoomEvent.Reply -> TODO()
            is RoomEvent.Redacted -> TODO()
            is RoomEvent.Message -> this.content.asString()
            is RoomEvent.Encrypted -> error("Should never happen")
        },
        eventId = this.eventId,
        timestampUtc = this.utcTimestamp,
    )

    private fun reducerWithInitialState(
        initialAttachments: List<MessageAttachment>?,
        block: suspend ReducerTestScope<MessengerScreenState, MessengerEvent>.() -> Unit
    ) = testReducer { fakeEventSource ->
        messengerReducer(
            fakeJobBag.instance,
            fakeChatEngine,
            fakeCopyToClipboard.instance,
            fakeDeviceMeta.instance,
            fakeMessageOptionsStore.instance,
            A_ROOM_ID,
            initialAttachments = initialAttachments,
            fakeEventSource,
        )
    }(block)

}

private fun aMessengerStateWithEvent(eventId: EventId, selfId: UserId) = aRoomStateWithEventId(eventId).toMessengerState(selfId)

private fun aRoomStateWithEventId(eventId: EventId): RoomState {
    val element = anEncryptedRoomMessageEvent(eventId = eventId, utcTimestamp = 1)
    return RoomState(aRoomOverview(roomId = A_ROOM_ID, isEncrypted = true), listOf(element))
}

private fun RoomState.toMessengerState(selfId: UserId) = aMessengerState(self = selfId, roomState = this)

class FakeCopyToClipboard {
    val instance = mockk<CopyToClipboard>()
}

class FakeDeviceMeta {
    val instance = mockk<DeviceMeta>()

    fun givenApiVersion() = every { instance.apiVersion }.delegateReturn()
}
