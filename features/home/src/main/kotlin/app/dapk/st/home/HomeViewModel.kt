package app.dapk.st.home

import androidx.lifecycle.viewModelScope
import app.dapk.st.directory.state.ComponentLifecycle
import app.dapk.st.directory.state.DirectorySideEffect
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.engine.ChatEngine
import app.dapk.st.home.HomeScreenState.*
import app.dapk.st.login.LoginViewModel
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.isSignedIn
import app.dapk.st.profile.state.ProfileAction
import app.dapk.st.profile.state.ProfileState
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val chatEngine: ChatEngine,
    private val directoryState: DirectoryState,
    private val loginViewModel: LoginViewModel,
    private val profileState: ProfileState,
    private val cacheCleaner: StoreCleaner,
    private val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
) : DapkViewModel<HomeScreenState, HomeEvent>(
    initialState = Loading
) {

    private var listenForInvitesJob: Job? = null

    fun directory() = directoryState
    fun login() = loginViewModel
    fun profile() = profileState

    fun start() {
        viewModelScope.launch {
            state = if (chatEngine.isSignedIn()) {
                _events.emit(HomeEvent.OnShowContent)
                initialHomeContent()
            } else {
                SignedOut
            }
        }

        viewModelScope.launch {
            if (chatEngine.isSignedIn()) {
                listenForInviteChanges()
            }
        }
    }

    private suspend fun initialHomeContent(): SignedIn {
        val me = chatEngine.me(forceRefresh = false)
        return when (val current = state) {
            Loading -> SignedIn(Page.Directory, me, invites = 0)
            is SignedIn -> current.copy(me = me, invites = current.invites)
            SignedOut -> SignedIn(Page.Directory, me, invites = 0)
        }
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
        directoryState.dispatch(DirectorySideEffect.ScrollToTop)
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

            Page.Profile -> {
                directoryState.dispatch(ComponentLifecycle.OnGone)
                profileState.dispatch(ProfileAction.Reset)
            }
        }
    }

    fun stop() {
        // do nothing
    }
}
