package app.dapk.st.messenger

import app.dapk.st.core.Lce
import app.dapk.st.design.components.BubbleModel
import app.dapk.st.engine.MessengerState
import app.dapk.st.engine.RoomEvent
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.navigator.MessageAttachment

data class MessengerScreenState(
    val roomId: RoomId?,
    val roomState: Lce<MessengerState>,
    val composerState: ComposerState,
    val viewerState: ViewerState?
)

data class ViewerState(
    val event: BubbleModel.Image,
)

sealed interface MessengerEvent {
    object SelectImageAttachment : MessengerEvent
    data class Toast(val message: String) : MessengerEvent
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
