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
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.SendMessage
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.asString
import app.dapk.st.messenger.CopyToClipboard
import app.dapk.st.navigator.MessageAttachment
import app.dapk.state.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
            composerState = initialAttachments?.let { ComposerState.Attachments(it, null) } ?: ComposerState.Text(value = "", reply = null),
            viewerState = null,
        ),

        async(ComponentLifecycle::class) { action ->
            val state = getState()
            when (action) {
                is ComponentLifecycle.Visible -> {
                    jobBag.add("messages", chatEngine.messages(state.roomId, disableReadReceipts = messageOptionsStore.isReadReceiptsDisabled())
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
                is ComposerStateChange.ReplyMode.Enter -> state.copy(
                    composerState = when (state.composerState) {
                        is ComposerState.Attachments -> state.composerState.copy(reply = action.replyingTo)
                        is ComposerState.Text -> state.composerState.copy(reply = action.replyingTo)
                    }
                )

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
                        val roomState = content.roomState
                        chatEngine.send(
                            message = SendMessage.TextMessage(
                                content = composerState.value,
                                reply = composerState.reply?.let {
                                    SendMessage.TextMessage.Reply(
                                        author = it.author,
                                        originalMessage = when (it) {
                                            is RoomEvent.Image -> TODO()
                                            is RoomEvent.Reply -> TODO()
                                            is RoomEvent.Message -> it.content.asString()
                                            is RoomEvent.Encrypted -> error("Should never happen")
                                        },
                                        eventId = it.eventId,
                                        timestampUtc = it.utcTimestamp,
                                    )
                                }
                            ),
                            room = roomState.roomOverview,
                        )
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
    )
}

private fun BubbleModel.findCopyableContent(): CopyableResult = when (this) {
    is BubbleModel.Encrypted -> CopyableResult.NothingToCopy
    is BubbleModel.Image -> CopyableResult.NothingToCopy
    is BubbleModel.Reply -> this.reply.findCopyableContent()
    is BubbleModel.Text -> CopyableResult.Content(CopyToClipboard.Copyable.Text(this.content.asString()))
}

private sealed interface CopyableResult {
    object NothingToCopy : CopyableResult
    data class Content(val value: CopyToClipboard.Copyable) : CopyableResult
}
