package app.dapk.st.home.state

import app.dapk.st.core.JobBag
import app.dapk.st.directory.state.ComponentLifecycle
import app.dapk.st.directory.state.DirectorySideEffect
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.engine.ChatEngine
import app.dapk.st.home.BetaVersionUpgradeUseCase
import app.dapk.st.profile.state.ProfileAction
import app.dapk.state.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun homeReducer(
    chatEngine: ChatEngine,
    cacheCleaner: StoreCleaner,
    betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
    jobBag: JobBag,
    eventEmitter: suspend (HomeEvent) -> Unit,
): ReducerFactory<HomeScreenState> {
    return createReducer(
        initialState = HomeScreenState.Loading,

        change(HomeAction.UpdateState::class) { action, _ ->
            action.state
        },

        change(HomeAction.UpdateToSignedIn::class) { action, state ->
            val me = action.me
            when (state) {
                HomeScreenState.Loading -> HomeScreenState.SignedIn(HomeScreenState.Page.Directory, me, invites = 0)
                is HomeScreenState.SignedIn -> state.copy(me = me, invites = state.invites)
                HomeScreenState.SignedOut -> HomeScreenState.SignedIn(HomeScreenState.Page.Directory, me, invites = 0)
            }
        },

        change(HomeAction.UpdateInvitesCount::class) { action, state ->
            when (state) {
                HomeScreenState.Loading -> state
                is HomeScreenState.SignedIn -> state.copy(invites = action.invitesCount)
                HomeScreenState.SignedOut -> state
            }
        },

        async(HomeAction.LifecycleVisible::class) { _ ->
            if (chatEngine.isSignedIn()) {
                eventEmitter.invoke(HomeEvent.OnShowContent)
                dispatch(HomeAction.InitialHome)
            } else {
                dispatch(HomeAction.UpdateState(HomeScreenState.SignedOut))
            }
        },

        async(HomeAction.InitialHome::class) {
            val me = chatEngine.me(forceRefresh = false)
            dispatch(HomeAction.UpdateToSignedIn(me))
            listenForInviteChanges(chatEngine, jobBag)
        },

        async(HomeAction.LoggedIn::class) {
            dispatch(HomeAction.InitialHome)
            eventEmitter.invoke(HomeEvent.OnShowContent)
        },

        async(HomeAction.ChangePageSideEffect::class) { action ->
            when (action.page) {
                HomeScreenState.Page.Directory -> {
                    // do nothing
                }

                HomeScreenState.Page.Profile -> {
                    dispatch(ComponentLifecycle.OnGone)
                    dispatch(ProfileAction.Reset)
                }
            }
        },

        multi(HomeAction.ChangePage::class) { action ->
            change { _, state ->
                when (state) {
                    is HomeScreenState.SignedIn -> when (action.page) {
                        state.page -> state
                        else -> state.copy(page = action.page)
                    }

                    HomeScreenState.Loading -> state
                    HomeScreenState.SignedOut -> state
                }
            }
            async {
                val state = getState()
                if (state is HomeScreenState.SignedIn && state.page != action.page) {
                    dispatch(HomeAction.ChangePageSideEffect(action.page))
                }
            }
        },

        async(HomeAction.ScrollToTop::class) {
            dispatch(DirectorySideEffect.ScrollToTop)
        },

        sideEffect(HomeAction.ClearCache::class) { _, _ ->
            cacheCleaner.cleanCache(removeCredentials = false)
            betaVersionUpgradeUseCase.notifyUpgraded()
            eventEmitter.invoke(HomeEvent.Relaunch)
        },
    )
}

private fun ReducerScope<HomeScreenState>.listenForInviteChanges(chatEngine: ChatEngine, jobBag: JobBag) {
    jobBag.replace(
        "invites-count",
        chatEngine.invites()
            .onEach { invites ->
                when (getState()) {
                    is HomeScreenState.SignedIn -> dispatch(HomeAction.UpdateInvitesCount(invites.size))
                    HomeScreenState.Loading,
                    HomeScreenState.SignedOut -> {
                        // do nothing
                    }
                }
            }.launchIn(coroutineScope)
    )
}