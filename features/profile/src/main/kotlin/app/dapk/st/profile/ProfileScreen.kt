package app.dapk.st.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
import app.dapk.st.design.components.Spider
import app.dapk.st.design.components.TextRow
import app.dapk.st.design.components.percentOfHeight
import app.dapk.st.settings.SettingsActivity

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    viewModel.ObserveEvents()

    LifecycleEffect(onStart = {
        viewModel.start()
    })

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), contentAlignment = Alignment.TopEnd
    ) {
        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
        }
    }

    when (val state = viewModel.state) {
        ProfileScreenState.Loading -> CenteredLoading()
        is ProfileScreenState.Content -> {
            val configuration = LocalConfiguration.current

            Column {
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val fallbackLabel = state.me.displayName ?: state.me.userId.value
                    val avatarSize = configuration.percentOfHeight(0.2f)
                    Box {
                        CircleishAvatar(state.me.avatarUrl?.value, fallbackLabel, avatarSize)

                        // TODO enable once edit support it added
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
                    content = state.me.displayName ?: "Not set",
                )
                TextRow(
                    title = "User id",
                    content = state.me.userId.value,
                )
                TextRow(
                    title = "Homeserver",
                    content = state.me.homeServerUrl.value,
                )

                TextRow(
                    title = "Invitations",
                    content = "${state.invitationsCount} pending",
                )
            }
        }
    }
}

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