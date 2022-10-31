package app.dapk.st.home

import androidx.lifecycle.viewModelScope
import app.dapk.st.directory.state.DirectorySideEffect
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.engine.ChatEngine
import app.dapk.st.home.HomeScreenState.*
import app.dapk.st.login.LoginViewModel
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.isSignedIn
import app.dapk.st.profile.ProfileViewModel
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatEngine: ChatEngine,
    private val credentialsProvider: CredentialsStore,
    private val directoryViewModel: DirectoryState,
    private val loginViewModel: LoginViewModel,
    private val profileViewModel: ProfileViewModel,
    private val cacheCleaner: StoreCleaner,
    private val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
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
                _events.emit(HomeEvent.OnShowContent)
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
        val me = chatEngine.me(forceRefresh = false)
        val initialInvites = chatEngine.invites().first().size
        return SignedIn(Page.Directory, me, invites = initialInvites)
    }

    fun loggedIn() {
        viewModelScope.launch {
            state = initialHomeContent()
            _events.emit(HomeEvent.OnShowContent)
            listenForInviteChanges()
        }
    }

    private fun CoroutineScope.listenForInviteChanges() {
        listenForInvitesJob?.cancel()
        listenForInvitesJob = chatEngine.invites()
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
            betaVersionUpgradeUseCase.notifyUpgraded()
            _events.emit(HomeEvent.Relaunch)
        }
    }

    fun scrollToTopOfMessages() {
        directoryViewModel.dispatch(DirectorySideEffect.ScrollToTop)
    }

    fun changePage(page: Page) {
        state = when (val current = state) {
            Loading -> current
            is SignedIn -> {
                when (page) {
                    current.page -> current
                    else -> current.copy(page = page).also {
                        pageChangeSideEffects(page)
                    }
                }
            }

            SignedOut -> current
        }
    }

    private fun pageChangeSideEffects(page: Page) {
        when (page) {
            Page.Directory -> {
                // do nothing
            }

            Page.Profile -> profileViewModel.reset()
        }
    }

    fun stop() {
        // do nothing
    }
}
