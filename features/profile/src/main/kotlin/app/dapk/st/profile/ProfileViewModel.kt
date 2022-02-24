package app.dapk.st.profile

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.DapkViewModel
import app.dapk.st.matrix.room.ProfileService
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileService: ProfileService,
) : DapkViewModel<ProfileScreenState, ProfileEvent>(
    initialState = ProfileScreenState.Loading
) {

    fun start() {
        viewModelScope.launch {
            val me = profileService.me(forceRefresh = true)
            state = ProfileScreenState.Content(me)
        }
    }


    fun updateDisplayName() {
        // TODO
    }

    fun updateAvatar() {
        // TODO
    }

}
