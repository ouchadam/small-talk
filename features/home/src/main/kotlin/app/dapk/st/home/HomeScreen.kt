package app.dapk.st.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.directory.DirectoryScreen
import app.dapk.st.home.HomeScreenState.*
import app.dapk.st.home.HomeScreenState.Page.Directory
import app.dapk.st.home.HomeScreenState.Page.Profile
import app.dapk.st.login.LoginScreen
import app.dapk.st.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel) {
    SmallTalkTheme {
        Surface(Modifier.fillMaxSize()) {
            LaunchedEffect(true) {
                homeViewModel.start()
            }


            when (val state = homeViewModel.state) {
                Loading -> CenteredLoading()
                is SignedIn -> {
                    Scaffold(
                        bottomBar = {
                            BottomBar(state, homeViewModel)
                        },
                        content = { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (state.page) {
                                    Directory -> DirectoryScreen(homeViewModel.directory())
                                    Profile -> {
                                        ProfileScreen(homeViewModel.profile()) {
                                            homeViewModel.changePage(Directory)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                SignedOut -> {
                    LoginScreen(homeViewModel.login()) {
                        homeViewModel.loggedIn()
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(state: SignedIn, homeViewModel: HomeViewModel) {
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
                                state.page == page -> homeViewModel.scrollToTopOfMessages()
                                else -> homeViewModel.changePage(page)
                            }
                        },
                    )
                    Profile -> NavigationBarItem(
                        icon = {
                            Box(modifier = Modifier.fillMaxHeight()) {
                                CircleishAvatar(state.me.avatarUrl?.value, state.me.displayName ?: state.me.userId.value, size = 25.dp)
                            }
                        },
                        selected = state.page == page,
                        onClick = { homeViewModel.changePage(page) },
                    )
                }
            }
        }
    }
}
