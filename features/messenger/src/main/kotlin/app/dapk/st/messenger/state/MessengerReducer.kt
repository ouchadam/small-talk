package app.dapk.st.messenger.state

import android.os.Build
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.JobBag
import app.dapk.st.core.Lce
import app.dapk.st.core.asString
import app.dapk.st.core.extensions.takeIfContent
import app.dapk.st.design.components.BubbleModel
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.MessengerPageState
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.SendMessage
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.asString
import app.dapk.st.messenger.CopyToClipboard
import app.dapk.st.navigator.MessageAttachment
import app.dapk.state.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.reflect.KClass

internal fun messengerReducer(
    jobBag: JobBag,
    chatEngine: ChatEngine,
    copyToClipboard: CopyToClipboard,
    deviceMeta: DeviceMeta,
    messageOptionsStore: MessageOptionsStore,
    roomId: RoomId,
    initialAttachments: List<MessageAttachment>?,
    eventEmitter: suspend (MessengerEvent) -> Unit,
): ReducerFactory<MessengerScreenState> {
    return createReducer(
        initialState = MessengerScreenState(
            roomId = roomId,
            roomState = Lce.Loading(),
            composerState = initialComposerState(initialAttachments),
            viewerState = null,
            dialogState = null,
        ),

        async(ComponentLifecycle::class) { action ->
            val state = getState()
            when (action) {
                is ComponentLifecycle.Visible -> {
                    jobBag.replace(
                        "messages", chatEngine.messages(state.roomId, disableReadReceipts = messageOptionsStore.isReadReceiptsDisabled())
                            .onEach { dispatch(MessagesStateChange.Content(it)) }
                            .launchIn(coroutineScope)
                    )
                }

                ComponentLifecycle.Gone -> jobBag.cancel("messages")
            }
        },

        change(MessagesStateChange.Content::class) { action, state ->
            state.copy(roomState = Lce.Content(action.content))
        },

        change(ComposerStateChange.SelectAttachmentToSend::class) { action, state ->
            state.copy(
                composerState = ComposerState.Attachments(
                    listOf(action.newValue),
                    state.composerState.reply,
                )
            )
        },

        change(ComposerStateChange.ImagePreview::class) { action, state ->
            when (action) {
                is ComposerStateChange.ImagePreview.Show -> state.copy(viewerState = ViewerState(action.image))
                ComposerStateChange.ImagePreview.Hide -> state.copy(viewerState = null)
            }
        },

        change(ComposerStateChange.TextUpdate::class) { action, state ->
            state.copy(composerState = ComposerState.Text(action.newValue, state.composerState.reply))
        },

        change(ComposerStateChange.Clear::class) { _, state ->
            state.copy(composerState = ComposerState.Text("", reply = null))
        },

        change(ComposerStateChange.ReplyMode::class) { action, state ->
            when (action) {
                is ComposerStateChange.ReplyMode.Enter -> {
                    when (action.replyingTo) {
                        is RoomEvent.Message -> state.copy(
                            composerState = when (state.composerState) {
                                is ComposerState.Attachments -> state.composerState.copy(reply = action.replyingTo)
                                is ComposerState.Text -> state.composerState.copy(reply = action.replyingTo)
                            }
                        )

                        // TODO support replying to more message types
                        is RoomEvent.Encrypted,
                        is RoomEvent.Image,
                        is RoomEvent.Redacted,
                        is RoomEvent.Reply -> state
                    }
                }

                ComposerStateChange.ReplyMode.Exit -> state.copy(
                    composerState = when (state.composerState) {
                        is ComposerState.Attachments -> state.composerState.copy(reply = null)
                        is ComposerState.Text -> state.composerState.copy(reply = null)
                    }
                )
            }
        },

        sideEffect(ScreenAction.CopyToClipboard::class) { action, state ->
            when (val result = action.model.findCopyableContent()) {
                is CopyableResult.Content -> {
                    copyToClipboard.copy(result.value)
                    if (deviceMeta.apiVersion <= Build.VERSION_CODES.S_V2) {
                        eventEmitter.invoke(MessengerEvent.Toast("Copied to clipboard"))
                    }
                }

                CopyableResult.NothingToCopy -> eventEmitter.invoke(MessengerEvent.Toast("Nothing to copy"))
            }
        },

        sideEffect(ScreenAction.OpenGalleryPicker::class) { _, _ ->
            eventEmitter.invoke(MessengerEvent.SelectImageAttachment)
        },

        async(ScreenAction.SendMessage::class) {
            val state = getState()
            when (val composerState = state.composerState) {
                is ComposerState.Text -> {
                    dispatch(ComposerStateChange.Clear)
                    state.roomState.takeIfContent()?.let { content ->
                        chatEngine.sendTextMessage(content, composerState)
                    }
                }

                is ComposerState.Attachments -> {
                    dispatch(ComposerStateChange.Clear)
                    state.roomState.takeIfContent()?.let { content ->
                        val roomState = content.roomState
                        chatEngine.send(SendMessage.ImageMessage(uri = composerState.values.first().uri.value), roomState.roomOverview)
                    }
                }
            }
        },

        change(MessagesStateChange.MuteContent::class) { action, state ->
            when (val roomState = state.roomState) {
                is Lce.Content -> state.copy(roomState = roomState.copy(value = roomState.value.copy(isMuted = action.isMuted)))
                is Lce.Error -> state
                is Lce.Loading -> state
            }
        },

        async(ScreenAction.Notifications::class) { action ->
            when (action) {
                ScreenAction.Notifications.Mute -> chatEngine.muteRoom(roomId)
                ScreenAction.Notifications.Unmute -> chatEngine.unmuteRoom(roomId)
            }

            dispatch(
                MessagesStateChange.MuteContent(
                    isMuted = when (action) {
                        ScreenAction.Notifications.Mute -> true
                        ScreenAction.Notifications.Unmute -> false
                    }
                )
            )
        },

        change(ScreenAction.UpdateDialogState::class) { action, state ->
            state.copy(dialogState = action.dialogState)
        },

        rewrite(ScreenAction.LeaveRoom::class) {
            ScreenAction.UpdateDialogState(
                DialogState.PositiveNegative(
                    title = "Leave room",
                    subtitle = "Are you sure you want you leave the room? If the room is private you will need to be invited again to rejoin.",
                    negativeAction = ScreenAction.LeaveRoomConfirmation.Deny,
                    positiveAction = ScreenAction.LeaveRoomConfirmation.Confirm,
                )
            )
        },

        async(ScreenAction.LeaveRoomConfirmation::class) { action ->
            dispatch(ScreenAction.UpdateDialogState(dialogState = null))

            when (action) {
                ScreenAction.LeaveRoomConfirmation.Confirm -> {
                    runCatching { chatEngine.rejectRoom(getState().roomId) }.fold(
                        onSuccess = { eventEmitter.invoke(MessengerEvent.OnLeftRoom) },
                        onFailure = { eventEmitter.invoke(MessengerEvent.Toast("Failed to leave room")) },
                    )
                }

                ScreenAction.LeaveRoomConfirmation.Deny -> {
                    // do nothing
                }
            }
        },
    )
}

private fun <A : Action, S> rewrite(klass: KClass<A>, mapper: (A) -> Action) = async<A, S>(klass) { action -> dispatch(mapper(action)) }

private suspend fun ChatEngine.sendTextMessage(content: MessengerPageState, composerState: ComposerState.Text) {
    val roomState = content.roomState
    val message = SendMessage.TextMessage(
        content = composerState.value,
        reply = composerState.reply?.toSendMessageReply(),
    )
    this.send(message = message, room = roomState.roomOverview)
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

private fun initialComposerState(initialAttachments: List<MessageAttachment>?) = initialAttachments
    ?.takeIf { it.isNotEmpty() }
    ?.let { ComposerState.Attachments(it, null) }
    ?: ComposerState.Text(value = "", reply = null)

private fun BubbleModel.findCopyableContent(): CopyableResult = when (this) {
    is BubbleModel.Encrypted -> CopyableResult.NothingToCopy
    is BubbleModel.Redacted -> CopyableResult.NothingToCopy
    is BubbleModel.Image -> CopyableResult.NothingToCopy
    is BubbleModel.Reply -> this.reply.findCopyableContent()
    is BubbleModel.Text -> CopyableResult.Content(CopyToClipboard.Copyable.Text(this.content.asString()))
}

private sealed interface CopyableResult {
    object NothingToCopy : CopyableResult
    data class Content(val value: CopyToClipboard.Copyable) : CopyableResult
}
