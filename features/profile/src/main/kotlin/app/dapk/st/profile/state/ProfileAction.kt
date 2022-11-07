package app.dapk.st.profile.state

import app.dapk.st.matrix.common.RoomId
import app.dapk.state.Action

sealed interface ProfileAction : Action {

    sealed interface ComponentLifecycle : ProfileAction {
        object Visible : ComponentLifecycle
        object Gone : ComponentLifecycle
    }

    object GoToInvitations : ProfileAction
    data class AcceptRoomInvite(val roomId: RoomId) : ProfileAction
    data class RejectRoomInvite(val roomId: RoomId) : ProfileAction
    object Reset : ProfileAction
}