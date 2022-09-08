package app.dapk.st.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : DapkActivity() {

    private val directoryViewModel by viewModel { module<DirectoryModule>().directoryViewModel() }
    private val loginViewModel by viewModel { module<LoginModule>().loginViewModel() }
    private val profileViewModel by viewModel { module<ProfileModule>().profileViewModel() }
    private val homeViewModel by viewModel { module<HomeModule>().homeViewModel(directoryViewModel, loginViewModel, profileViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel.events.onEach {
            when (it) {
                HomeEvent.Relaunch -> recreate()
            }
        }.launchIn(lifecycleScope)

        setContent {
            if (homeViewModel.hasVersionChanged()) {
                BetaUpgradeDialog()
            } else {
                HomeScreen(homeViewModel)
            }
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
