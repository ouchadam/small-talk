package app.dapk.st.home.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import app.dapk.st.engine.Me
import app.dapk.st.state.State

typealias HomeState = State<HomeScreenState, HomeEvent>

sealed interface HomeScreenState {

    object Loading : HomeScreenState
    object SignedOut : HomeScreenState
    data class SignedIn(val page: Page, val me: Me, val invites: Int) : HomeScreenState

    enum class Page(val icon: ImageVector) {
        Directory(Icons.Filled.Menu),
        Profile(Icons.Filled.Settings)
    }

}

sealed interface HomeEvent {
    object Relaunch : HomeEvent
    object OnShowContent : HomeEvent
}

