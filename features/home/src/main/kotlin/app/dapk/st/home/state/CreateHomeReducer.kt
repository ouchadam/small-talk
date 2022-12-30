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

fun createHomeReducer(
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
                listenForInviteChanges(chatEngine, jobBag)
            } else {
                dispatch(HomeAction.UpdateState(HomeScreenState.SignedOut))
            }
        },

        async(HomeAction.InitialHome::class) {
            val me = chatEngine.me(forceRefresh = false)
            val nextState = when (val current = getState()) {
                HomeScreenState.Loading -> HomeScreenState.SignedIn(HomeScreenState.Page.Directory, me, invites = 0)
                is HomeScreenState.SignedIn -> current.copy(me = me, invites = current.invites)
                HomeScreenState.SignedOut -> HomeScreenState.SignedIn(HomeScreenState.Page.Directory, me, invites = 0)
            }
            dispatch(HomeAction.UpdateState(nextState))
        },

        async(HomeAction.LoggedIn::class) {
            dispatch(HomeAction.InitialHome)
            eventEmitter.invoke(HomeEvent.OnShowContent)
            listenForInviteChanges(chatEngine, jobBag)
        },

        multi(HomeAction.ChangePage::class) { action ->
            change { _, state ->
                when (state) {
                    is HomeScreenState.SignedIn -> when (action.page) {
                        state.page -> state
                        else -> state.copy(page = action.page).also {
                            async {
                                when (action.page) {
                                    HomeScreenState.Page.Directory -> {
                                        // do nothing
                                    }

                                    HomeScreenState.Page.Profile -> {
                                        dispatch(ComponentLifecycle.OnGone)
                                        dispatch(ProfileAction.Reset)
                                    }
                                }
                            }
                        }
                    }

                    HomeScreenState.Loading -> state
                    HomeScreenState.SignedOut -> state
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
        }
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