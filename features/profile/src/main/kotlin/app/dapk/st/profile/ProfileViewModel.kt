package app.dapk.st.profile

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.DapkViewModel
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileService: ProfileService,
    private val syncService: SyncService,
) : DapkViewModel<ProfileScreenState, ProfileEvent>(
    initialState = ProfileScreenState.Loading
) {

    fun start() {
        viewModelScope.launch {
            val invitationsCount = syncService.invites().firstOrNull()?.size ?: 0
            val me = profileService.me(forceRefresh = true)
            state = ProfileScreenState.Content(me, invitationsCount = invitationsCount)
        }
    }


    fun updateDisplayName() {
        // TODO
    }

    fun updateAvatar() {
        // TODO
    }

}
