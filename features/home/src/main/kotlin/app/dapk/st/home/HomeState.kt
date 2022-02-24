package app.dapk.st.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import app.dapk.st.matrix.room.ProfileService

sealed interface HomeScreenState {

    object Loading : HomeScreenState
    object SignedOut : HomeScreenState
    data class SignedIn(val page: Page, val me: ProfileService.Me) : HomeScreenState

    enum class Page(val icon: ImageVector) {
        Directory(Icons.Filled.Menu),
        Profile(Icons.Filled.Settings)
    }

}

sealed interface HomeEvent

