package app.dapk.st.home

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.state.state
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : DapkActivity() {

    private val directoryViewModel by state { module<DirectoryModule>().directoryState() }
    private val loginViewModel by viewModel { module<LoginModule>().loginViewModel() }
    private val profileViewModel by state { module<ProfileModule>().profileState() }
    private val homeViewModel by viewModel { module<HomeModule>().homeViewModel(directoryViewModel, loginViewModel, profileViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pushPermissionLauncher = registerPushPermission()
        homeViewModel.events.onEach {
            when (it) {
                HomeEvent.Relaunch -> recreate()
                HomeEvent.OnShowContent -> pushPermissionLauncher?.invoke()
            }
        }.launchIn(lifecycleScope)

        setContent {
            if (homeViewModel.hasVersionChanged()) {
                BetaUpgradeDialog()
            } else {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen(homeViewModel)
                }
            }
        }
    }

    private fun registerPushPermission(): (() -> Unit)? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }
    }

    @Composable
    private fun BetaUpgradeDialog() {
        AlertDialog(
            title = { Text(text = "BETA") },
            text = { Text(text = "During the BETA, version upgrades require a cache clear") },
            onDismissRequest = {

            },
            confirmButton = {
                TextButton(onClick = { homeViewModel.clearCache() }) {
                    Text(text = "Clear cache".uppercase())
                }
            },
        )
    }
}
