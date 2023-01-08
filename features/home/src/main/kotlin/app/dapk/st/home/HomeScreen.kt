package app.dapk.st.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
import app.dapk.st.directory.DirectoryScreen
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.home.state.HomeAction
import app.dapk.st.home.state.HomeScreenState.*
import app.dapk.st.home.state.HomeScreenState.Page.Directory
import app.dapk.st.home.state.HomeScreenState.Page.Profile
import app.dapk.st.home.state.HomeState
import app.dapk.st.login.LoginScreen
import app.dapk.st.login.state.LoginState
import app.dapk.st.profile.ProfileScreen
import app.dapk.st.profile.state.ProfileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(homeState: HomeState, directoryState: DirectoryState, loginState: LoginState, profileState: ProfileState) {
    LifecycleEffect(
        onStart = { homeState.dispatch(HomeAction.LifecycleVisible) },
        onStop = { homeState.dispatch(HomeAction.LifecycleGone) }
    )

    when (val state = homeState.current) {
        Loading -> CenteredLoading()
        is SignedIn -> {
            Scaffold(
                bottomBar = {
                    BottomBar(state, homeState)
                },
                content = { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (state.page) {
                            Directory -> DirectoryScreen(directoryState)
                            Profile -> {
                                ProfileScreen(profileState) {
                                    homeState.dispatch(HomeAction.ChangePage(Directory))
                                }
                            }
                        }
                    }
                }
            )
        }

        SignedOut -> {
            LoginScreen(loginState) {
                homeState.dispatch(HomeAction.LoggedIn)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomBar(state: SignedIn, homeState: HomeState) {
    Column {
        Divider(modifier = Modifier.fillMaxWidth(), color = Color.Black.copy(alpha = 0.2f), thickness = 0.5.dp)
        NavigationBar(containerColor = Color.Transparent, modifier = Modifier.height(IntrinsicSize.Min)) {
            Page.values().forEach { page ->
                when (page) {
                    Directory -> NavigationBarItem(
                        icon = { Icon(page.icon, contentDescription = null) },
                        selected = state.page == page,
                        onClick = {
                            when {
                                state.page == page -> homeState.dispatch(HomeAction.ScrollToTop)
                                else -> homeState.dispatch(HomeAction.ChangePage(page))
                            }
                        },
                    )

                    Profile -> NavigationBarItem(
                        icon = {
                            BadgedBox(badge = {
                                if (state.invites > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("!", color = MaterialTheme.colorScheme.onPrimary) }
                                }
                            }) {
                                Box {
                                    CircleishAvatar(state.me.avatarUrl?.value, state.me.displayName ?: state.me.userId.value, size = 25.dp)
                                }
                            }
                        },
                        selected = state.page == page,
                        onClick = { homeState.dispatch(HomeAction.ChangePage(page)) },
                    )
                }
            }
        }
    }
}
