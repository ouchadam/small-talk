package app.dapk.st.home

import androidx.lifecycle.viewModelScope
import app.dapk.st.directory.DirectoryViewModel
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.home.HomeScreenState.*
import app.dapk.st.login.LoginViewModel
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.isSignedIn
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.profile.ProfileViewModel
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val credentialsProvider: CredentialsStore,
    private val directoryViewModel: DirectoryViewModel,
    private val loginViewModel: LoginViewModel,
    private val profileViewModel: ProfileViewModel,
    private val profileService: ProfileService,
    private val cacheCleaner: StoreCleaner,
    private val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
    private val syncService: SyncService,
) : DapkViewModel<HomeScreenState, HomeEvent>(
    initialState = Loading
) {

    private var listenForInvitesJob: Job? = null

    fun directory() = directoryViewModel
    fun login() = loginViewModel
    fun profile() = profileViewModel

    fun start() {
        viewModelScope.launch {
            state = if (credentialsProvider.isSignedIn()) {
                initialHomeContent()
            } else {
                SignedOut
            }
        }

        viewModelScope.launch {
            if (credentialsProvider.isSignedIn()) {
                listenForInviteChanges()
            }
        }

    }

    private suspend fun initialHomeContent(): SignedIn {
        val me = profileService.me(forceRefresh = false)
        val initialInvites = syncService.invites().first().size
        return SignedIn(Page.Directory, me, invites = initialInvites)
    }

    fun loggedIn() {
        viewModelScope.launch {
            state = initialHomeContent()
            listenForInviteChanges()
        }
    }

    private fun CoroutineScope.listenForInviteChanges() {
        listenForInvitesJob?.cancel()
        listenForInvitesJob = syncService.invites()
            .onEach { invites ->
                when (val currentState = state) {
                    is SignedIn -> updateState { currentState.copy(invites = invites.size) }
                    Loading,
                    SignedOut -> {
                        // do nothing
                    }
                }
            }.launchIn(this)
    }

    fun hasVersionChanged() = betaVersionUpgradeUseCase.hasVersionChanged()

    fun clearCache() {
        viewModelScope.launch {
            cacheCleaner.cleanCache(removeCredentials = false)
            _events.emit(HomeEvent.Relaunch)
        }
    }

    fun scrollToTopOfMessages() {
        directoryViewModel.scrollToTopOfMessages()
    }

    fun changePage(page: Page) {
        state = when (val current = state) {
            Loading -> current
            is SignedIn -> current.copy(page = page)
            SignedOut -> current
        }
    }

    fun stop() {
        viewModelScope.cancel()
    }
}
