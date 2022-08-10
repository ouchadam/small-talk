package app.dapk.st.messenger

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.takeIfContent
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
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
    private val clock: Clock,
    factory: MutableStateFactory<MessengerScreenState> = defaultStateFactory(),
) : DapkViewModel<MessengerScreenState, MessengerEvent>(
    initialState = MessengerScreenState(
        roomId = null,
        roomState = Lce.Loading(),
        composerState = ComposerState.Text(value = "")
    ),
    factory = factory,
) {

    private var syncJob: Job? = null

    fun post(action: MessengerAction) {
        when (action) {
            is MessengerAction.OnMessengerVisible -> start(action)
            MessengerAction.OnMessengerGone -> syncJob?.cancel()
            is MessengerAction.ComposerTextUpdate -> updateState { copy(composerState = ComposerState.Text(action.newValue)) }
            MessengerAction.ComposerSendText -> sendMessage()
            MessengerAction.ComposerClear -> updateState { copy(composerState = ComposerState.Text("")) }
        }
    }

    private fun start(action: MessengerAction.OnMessengerVisible) {
        updateState { copy(roomId = action.roomId, composerState = action.attachments?.let { ComposerState.Attachments(it) } ?: composerState) }
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
                roomService.markFullyRead(state.roomState.roomOverview.roomId, latestReadEvent)
                roomStore.markRead(state.roomState.roomOverview.roomId)
            }
        }
    }

    private fun sendMessage() {
        when (val composerState = state.composerState) {
            is ComposerState.Text -> {
                val copy = composerState.copy()
                updateState { copy(composerState = composerState.copy(value = "")) }

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
                            )
                        )
                    }
                }
            }
            is ComposerState.Attachments -> {
                val copy = composerState.copy()
                updateState { copy(composerState = ComposerState.Text("")) }

                state.roomState.takeIfContent()?.let { content ->
                    val roomState = content.roomState
                    viewModelScope.launch {
                        messageService.scheduleMessage(
                            MessageService.Message.ImageMessage(
                                MessageService.Message.Content.ApiImageContent(uri = copy.values.first().uri.value),
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

}

private fun MessengerState.latestMessageEventFromOthers(self: UserId) = this.roomState.events
    .filterIsInstance<RoomEvent.Message>()
    .filterNot { it.author.id == self }
    .firstOrNull()
    ?.eventId

sealed interface MessengerAction {
    data class ComposerTextUpdate(val newValue: String) : MessengerAction
    object ComposerSendText : MessengerAction
    object ComposerClear : MessengerAction
    data class OnMessengerVisible(val roomId: RoomId, val attachments: List<MessageAttachment>?) : MessengerAction
    object OnMessengerGone : MessengerAction
}