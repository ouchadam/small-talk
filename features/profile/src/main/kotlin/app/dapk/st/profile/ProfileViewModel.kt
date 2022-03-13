package app.dapk.st.profile

import androidx.lifecycle.viewModelScope
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ProfileViewModel(
    private val profileService: ProfileService,
    private val syncService: SyncService,
) : DapkViewModel<ProfileScreenState, ProfileEvent>(
    initialState = ProfileScreenState.Loading
) {

    private var syncingJob: Job? = null

    fun start() {
        syncingJob = combine(
            syncService.startSyncing(),
            flow { emit(profileService.me(forceRefresh = true)) },
            syncService.invites(),
            transform = { _, me, invites -> me to invites }
        )
            .onEach { (me, invites) -> state = ProfileScreenState.Content(me, invitationsCount = invites.size) }
            .launchIn(viewModelScope)
    }

    fun updateDisplayName() {
        // TODO
    }

    fun updateAvatar() {
        // TODO
    }

    fun stop() {
        syncingJob?.cancel()
    }

}
