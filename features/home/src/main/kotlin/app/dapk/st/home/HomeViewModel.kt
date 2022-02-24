package app.dapk.st.home

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.DapkViewModel
import app.dapk.st.directory.DirectoryViewModel
import app.dapk.st.home.HomeScreenState.*
import app.dapk.st.login.LoginViewModel
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.isSignedIn
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.profile.ProfileViewModel
import kotlinx.coroutines.launch

class HomeViewModel(
    private val credentialsProvider: CredentialsStore,
    private val directoryViewModel: DirectoryViewModel,
    private val loginViewModel: LoginViewModel,
    private val profileViewModel: ProfileViewModel,
    private val profileService: ProfileService,
) : DapkViewModel<HomeScreenState, HomeEvent>(
    initialState = Loading
) {

    fun directory() = directoryViewModel
    fun login() = loginViewModel
    fun profile() = profileViewModel

    fun start() {
        viewModelScope.launch {
            state = if (credentialsProvider.isSignedIn()) {
                val me = profileService.me(forceRefresh = false)
                SignedIn(Page.Directory, me)
            } else {
                SignedOut
            }
        }
    }

    fun loggedIn() {
        viewModelScope.launch {
            val me = profileService.me(forceRefresh = false)
            state = SignedIn(Page.Directory, me)
        }
    }

    fun loggedOut() {
        state = SignedOut
    }

    fun changePage(page: Page) {
        state = when (val current = state) {
            Loading -> current
            is SignedIn -> current.copy(page = page)
            SignedOut -> current
        }
    }
}
