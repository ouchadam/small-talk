package app.dapk.st.home.state

import app.dapk.st.engine.Me
import app.dapk.st.home.state.HomeScreenState.Page
import app.dapk.state.Action

sealed interface HomeAction : Action {
    object LifecycleVisible : HomeAction
    object LifecycleGone : HomeAction

    object ScrollToTop : HomeAction
    object ClearCache : HomeAction
    object LoggedIn : HomeAction

    data class ChangePage(val page: Page) : HomeAction
    data class ChangePageSideEffect(val page: Page) : HomeAction
    data class UpdateInvitesCount(val invitesCount: Int) : HomeAction
    data class UpdateToSignedIn(val me: Me) : HomeAction
    data class UpdateState(val state: HomeScreenState) : HomeAction
    object InitialHome : HomeAction
}
