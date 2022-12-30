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
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.core.module
import app.dapk.st.home.state.HomeAction
import app.dapk.st.home.state.HomeEvent
import app.dapk.st.home.state.HomeState
import app.dapk.st.state.state
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : DapkActivity() {

    private val homeModule by unsafeLazy { module<HomeModule>() }
    private val compositeState by state { homeModule.compositeHomeState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pushPermissionLauncher = registerPushPermission()
        compositeState.events.onEach {
            when (it) {
                HomeEvent.Relaunch -> recreate()
                HomeEvent.OnShowContent -> pushPermissionLauncher?.invoke()
            }
        }.launchIn(lifecycleScope)

        setContent {
            val homeState: HomeState = compositeState.childState()
            if (homeModule.betaVersionUpgradeUseCase.hasVersionChanged()) {
                BetaUpgradeDialog(homeState)
            } else {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen(homeState, compositeState.childState(), compositeState.childState(), compositeState.childState())
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
}

@Composable
private fun BetaUpgradeDialog(homeState: HomeState) {
    AlertDialog(
        title = { Text(text = "BETA") },
        text = { Text(text = "During the BETA, version upgrades require a cache clear") },
        onDismissRequest = {

        },
        confirmButton = {
            TextButton(onClick = { homeState.dispatch(HomeAction.ClearCache) }) {
                Text(text = "Clear cache".uppercase())
            }
        },
    )
}
