package app.dapk.st.profile

import app.dapk.st.matrix.room.ProfileService

sealed interface ProfileScreenState {
    object Loading : ProfileScreenState
    data class Content(val me: ProfileService.Me) : ProfileScreenState

}

sealed interface ProfileEvent {

}

