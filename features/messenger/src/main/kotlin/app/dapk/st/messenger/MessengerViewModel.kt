package app.dapk.st.messenger

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.takeIfContent
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.viewmodel.DapkViewModel
import app.dapk.st.viewmodel.MutableStateFactory
import app.dapk.st.viewmodel.defaultStateFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import java.time.Clock

internal class MessengerViewModel(
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val roomStore: RoomStore,
    private val credentialsStore: CredentialsStore,
    private val observeTimeline: ObserveTimelineUseCase,
    private val localIdFactory: LocalIdFactory,
    private val imageContentReader: ImageContentReader,
    private val clock: Clock,
    factory: MutableStateFactory<MessengerScreenState> = defaultStateFactory(),
) : DapkViewModel<MessengerScreenState, MessengerEvent>(
    initialState = MessengerScreenState(
        roomId = null,
        roomState = Lce.Loading(),
        composerState = ComposerState.Text(value = "", reply = null)
    ),
    factory = factory,
) {

    private var syncJob: Job? = null

    fun post(action: MessengerAction) {
        when (action) {
            is MessengerAction.OnMessengerVisible -> start(action)
            MessengerAction.OnMessengerGone -> syncJob?.cancel()
            is MessengerAction.ComposerTextUpdate -> updateState { copy(composerState = ComposerState.Text(action.newValue, composerState.reply)) }
            MessengerAction.ComposerSendText -> sendMessage()
            MessengerAction.ComposerClear -> resetComposer()
            is MessengerAction.ComposerImageUpdate -> updateState {
                copy(
                    composerState = ComposerState.Attachments(
                        listOf(action.newValue),
                        composerState.reply
                    )
                )
            }

            is MessengerAction.ComposerEnterReplyMode -> updateState {
                copy(
                    composerState = when (composerState) {
                        is ComposerState.Attachments -> composerState.copy(reply = action.replyingTo)
                        is ComposerState.Text -> composerState.copy(reply = action.replyingTo)
                    }
                )
            }

            MessengerAction.ComposerExitReplyMode -> updateState {
                copy(
                    composerState = when (composerState) {
                        is ComposerState.Attachments -> composerState.copy(reply = null)
                        is ComposerState.Text -> composerState.copy(reply = null)
                    }
                )
            }
        }
    }

    private fun start(action: MessengerAction.OnMessengerVisible) {
        updateState { copy(roomId = action.roomId, composerState = action.attachments?.let { ComposerState.Attachments(it, null) } ?: composerState) }
        syncJob = viewModelScope.launch {
            roomStore.markRead(action.roomId)

            val credentials = credentialsStore.credentials()!!
            var lastKnownReadEvent: EventId? = null
            observeTimeline.invoke(action.roomId, credentials.userId).distinctUntilChanged().onEach { state ->
                state.latestMessageEventFromOthers(self = credentials.userId)?.let {
                    if (lastKnownReadEvent != it) {
                        updateRoomReadStateAsync(latestReadEvent = it, state)
                        lastKnownReadEvent = it
                    }
                }
                updateState { copy(roomState = Lce.Content(state)) }
            }.collect()
        }
    }

    private fun CoroutineScope.updateRoomReadStateAsync(latestReadEvent: EventId, state: MessengerState): Deferred<Unit> {
        return async {
            runCatching {
                roomService.markFullyRead(state.roomState.roomOverview.roomId, latestReadEvent, isPrivate = true)
                roomStore.markRead(state.roomState.roomOverview.roomId)
            }
        }
    }

    private fun sendMessage() {
        when (val composerState = state.composerState) {
            is ComposerState.Text -> {
                val copy = composerState.copy()
                updateState { copy(composerState = composerState.copy(value = "", reply = null)) }

                state.roomState.takeIfContent()?.let { content ->
                    val roomState = content.roomState
                    viewModelScope.launch {
                        messageService.scheduleMessage(
                            MessageService.Message.TextMessage(
                                MessageService.Message.Content.TextContent(body = copy.value),
                                roomId = roomState.roomOverview.roomId,
                                sendEncrypted = roomState.roomOverview.isEncrypted,
                                localId = localIdFactory.create(),
                                timestampUtc = clock.millis(),
                                reply = copy.reply?.let {
                                    MessageService.Message.TextMessage.Reply(
                                        author = it.author,
                                        originalMessage = when (it) {
                                            is RoomEvent.Image -> TODO()
                                            is RoomEvent.Reply -> TODO()
                                            is RoomEvent.Message -> it.content
                                        },
                                        replyContent = copy.value,
                                        eventId = it.eventId,
                                        timestampUtc = it.utcTimestamp,
                                    )
                                }
                            )
                        )
                    }
                }
            }

            is ComposerState.Attachments -> {
                val copy = composerState.copy()
                resetComposer()

                state.roomState.takeIfContent()?.let { content ->
                    val roomState = content.roomState
                    viewModelScope.launch {
                        val imageUri = copy.values.first().uri.value
                        val meta = imageContentReader.meta(imageUri)

                        messageService.scheduleMessage(
                            MessageService.Message.ImageMessage(
                                MessageService.Message.Content.ImageContent(
                                    uri = imageUri, MessageService.Message.Content.ImageContent.Meta(
                                        height = meta.height,
                                        width = meta.width,
                                        size = meta.size,
                                        fileName = meta.fileName,
                                        mimeType = meta.mimeType,
                                    )
                                ),
                                roomId = roomState.roomOverview.roomId,
                                sendEncrypted = roomState.roomOverview.isEncrypted,
                                localId = localIdFactory.create(),
                                timestampUtc = clock.millis(),
                            )
                        )
                    }
                }

            }
        }
    }

    private fun resetComposer() {
        updateState { copy(composerState = ComposerState.Text("", reply = null)) }
    }

    fun startAttachment() {
        viewModelScope.launch {
            _events.emit(MessengerEvent.SelectImageAttachment)
        }
    }

}

private fun MessengerState.latestMessageEventFromOthers(self: UserId) = this.roomState.events
    .filterIsInstance<RoomEvent.Message>()
    .filterNot { it.author.id == self }
    .firstOrNull()
    ?.eventId

sealed interface MessengerAction {
    data class ComposerTextUpdate(val newValue: String) : MessengerAction
    data class ComposerEnterReplyMode(val replyingTo: RoomEvent) : MessengerAction
    object ComposerExitReplyMode : MessengerAction
    data class ComposerImageUpdate(val newValue: MessageAttachment) : MessengerAction
    object ComposerSendText : MessengerAction
    object ComposerClear : MessengerAction
    data class OnMessengerVisible(val roomId: RoomId, val attachments: List<MessageAttachment>?) : MessengerAction
    object OnMessengerGone : MessengerAction
}