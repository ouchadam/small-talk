package app.dapk.st.settings

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.resetModules
import app.dapk.st.settings.state.SettingsState
import app.dapk.st.state.state

class SettingsActivity : DapkActivity() {

    private val settingsState: SettingsState by state { module<SettingsModule>().settingsState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(Modifier.fillMaxSize()) {
                SettingsScreen(settingsState, onSignOut = {
                    resetModules()
                    navigator.navigate.toHome()
                    finish()
                }, navigator)
            }
        }
    }
}
