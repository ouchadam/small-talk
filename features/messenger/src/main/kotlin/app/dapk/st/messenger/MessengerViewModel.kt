package app.dapk.st.messenger

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.takeIfContent
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MessengerViewModel(
    syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val roomStore: RoomStore,
    private val credentialsStore: CredentialsStore,
) : DapkViewModel<MessengerScreenState, MessengerEvent>(
    initialState = MessengerScreenState(
        roomId = null,
        roomState = Lce.Loading(),
        composerState = ComposerState.Text(value = "")
    )
) {

    private var syncJob: Job? = null
    private val useCase: TimelineUseCase = TimelineUseCase(syncService, messageService, roomService, MergeWithLocalEchosUseCaseImpl())

    fun post(action: MessengerAction) {
        when (action) {
            is MessengerAction.ComposerTextUpdate -> {
                updateState { copy(composerState = ComposerState.Text(action.newValue)) }
            }
            is MessengerAction.OnMessengerVisible -> {
                updateState { copy(roomId = action.roomId) }

                syncJob = viewModelScope.launch {
                    useCase.startSyncing().collect()
                }
                viewModelScope.launch {
                    roomStore.markRead(action.roomId)

                    val credentials = credentialsStore.credentials()!!
                    useCase.state(action.roomId, credentials.userId).distinctUntilChanged().onEach { state ->
                        state.roomState.events.filterIsInstance<RoomEvent.Message>().filterNot { it.author.id == credentials.userId }.firstOrNull()?.let {
                            roomService.markFullyRead(state.roomState.roomOverview.roomId, it.eventId)
                            roomStore.markRead(state.roomState.roomOverview.roomId)
                        }
                        updateState { copy(roomState = Lce.Content(state)) }
                    }.collect()
                }
            }
            MessengerAction.OnMessengerGone -> {
                syncJob?.cancel()
            }
            MessengerAction.ComposerSendText -> {
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
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

sealed interface MessengerAction {
    data class ComposerTextUpdate(val newValue: String) : MessengerAction
    object ComposerSendText : MessengerAction
    data class OnMessengerVisible(val roomId: RoomId) : MessengerAction
    object OnMessengerGone : MessengerAction
}