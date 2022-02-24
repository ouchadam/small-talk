package app.dapk.st.messenger

import app.dapk.st.core.Lce
import app.dapk.st.matrix.common.RoomId

data class MessengerScreenState(
    val roomId: RoomId?,
    val roomState: Lce<MessengerState>,
    val composerState: ComposerState,
)

sealed interface MessengerEvent

sealed interface ComposerState {

    data class Text(
        val value: String,
    ) : ComposerState

}
