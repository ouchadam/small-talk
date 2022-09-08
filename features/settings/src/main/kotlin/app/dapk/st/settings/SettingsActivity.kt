package app.dapk.st.settings

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.resetModules
import app.dapk.st.core.viewModel

class SettingsActivity : DapkActivity() {

    private val settingsViewModel by viewModel { module<SettingsModule>().settingsViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(Modifier.fillMaxSize()) {
                SettingsScreen(settingsViewModel, onSignOut = {
                    resetModules()
                    navigator.navigate.toHome()
                    finish()
                }, navigator)
            }
        }
    }
}
