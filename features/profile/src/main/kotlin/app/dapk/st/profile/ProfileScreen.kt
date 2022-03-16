package app.dapk.st.profile

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.dapk.st.core.Lce
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.*
import app.dapk.st.matrix.sync.InviteMeta
import app.dapk.st.matrix.sync.RoomInvite
import app.dapk.st.settings.SettingsActivity

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onTopLevelBack: () -> Unit) {
    viewModel.ObserveEvents()

    LifecycleEffect(
        onStart = { viewModel.start() },
        onStop = { viewModel.stop() }
    )

    val context = LocalContext.current

    val onNavigate: (SpiderPage<out Page>?) -> Unit = {
        when (it) {
            null -> onTopLevelBack()
            else -> viewModel.goTo(it)
        }
    }
    Spider(currentPage = viewModel.state.page, onNavigate = onNavigate) {
        item(Page.Routes.profile) {
            ProfilePage(context, viewModel, it)
        }
        item(Page.Routes.invitation) {
            Invitations(viewModel, it)
        }
    }
}

@Composable
private fun ProfilePage(context: Context, viewModel: ProfileViewModel, profile: Page.Profile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), contentAlignment = Alignment.TopEnd
    ) {
        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
        }
    }

    when (val state = profile.content) {
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            val configuration = LocalConfiguration.current
            val content = state.value
            Column {
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val fallbackLabel = content.me.displayName ?: content.me.userId.value
                    val avatarSize = configuration.percentOfHeight(0.2f)
                    Box {
                        CircleishAvatar(content.me.avatarUrl?.value, fallbackLabel, avatarSize)

                        // TODO enable once edit support is added
                        if (false) {
                            IconButton(modifier = Modifier
                                .size(avatarSize * 0.314f)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colors.primary, shape = CircleShape)
                                .padding(12.dp),
                                onClick = {}
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colors.onPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                TextRow(
                    title = "Display name",
                    content = content.me.displayName ?: "Not set",
                )
                TextRow(
                    title = "User id",
                    content = content.me.userId.value,
                )
                TextRow(
                    title = "Homeserver",
                    content = content.me.homeServerUrl.value,
                )

                TextRow(
                    title = "Invitations",
                    content = "${content.invitationsCount} pending",
                    onClick = { viewModel.goToInvitations() }
                )
            }
        }
    }
}

@Composable
private fun Invitations(viewModel: ProfileViewModel, invitations: Page.Invitations) {
    when (val state = invitations.content) {
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            LazyColumn {
                items(state.value) {
                    val text = when (val meta = it.inviteMeta) {
                        InviteMeta.DirectMessage -> "${it.inviterName()} has invited you to chat"
                        is InviteMeta.Room -> "${it.inviterName()} has invited you to ${meta.roomName ?: "unnamed room"}"
                    }

                    TextRow(title = text, includeDivider = false) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Button(modifier = Modifier.weight(1f), onClick = { viewModel.rejectRoomInvite(it.roomId) }) {
                                Text("Reject".uppercase())
                            }
                            Spacer(modifier = Modifier.fillMaxWidth(0.1f))
                            Button(modifier = Modifier.weight(1f), onClick = { viewModel.acceptRoomInvite(it.roomId) }) {
                                Text("Accept".uppercase())
                            }
                        }
                    }

                }
            }
        }
        is Lce.Error -> TODO()
    }
}

private fun RoomInvite.inviterName() = this.from.displayName?.let { "$it (${this.from.id.value})" } ?: this.from.id.value

@Composable
private fun ProfileViewModel.ObserveEvents() {
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
            }
        }
    }
}