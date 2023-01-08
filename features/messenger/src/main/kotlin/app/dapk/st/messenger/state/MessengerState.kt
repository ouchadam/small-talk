package app.dapk.st.messenger.state

import app.dapk.st.core.Lce
import app.dapk.st.design.components.BubbleModel
import app.dapk.st.engine.MessengerPageState
import app.dapk.st.engine.RoomEvent
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.state.State
import app.dapk.state.Action

typealias MessengerState = State<MessengerScreenState, MessengerEvent>

data class MessengerScreenState(
    val roomId: RoomId,
    val roomState: Lce<MessengerPageState>,
    val composerState: ComposerState,
    val viewerState: ViewerState?,
    val dialogState: DialogState?,
)

data class ViewerState(
    val event: BubbleModel.Image,
)

sealed interface DialogState {
    data class PositiveNegative(
        val title: String,
        val subtitle: String,
        val positiveAction: Action,
        val negativeAction: Action,
    ) : DialogState
}

sealed interface MessengerEvent {
    object SelectImageAttachment : MessengerEvent
    data class Toast(val message: String) : MessengerEvent
    object OnLeftRoom : MessengerEvent
}

sealed interface ComposerState {

    val reply: RoomEvent?

    data class Text(
        val value: String,
        override val reply: RoomEvent?,
    ) : ComposerState

    data class Attachments(
        val values: List<MessageAttachment>,
        override val reply: RoomEvent?,
    ) : ComposerState

}
