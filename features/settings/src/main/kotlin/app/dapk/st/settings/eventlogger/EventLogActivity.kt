package app.dapk.st.settings.eventlogger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.settings.SettingsModule
import app.dapk.st.design.components.SmallTalkTheme

class EventLogActivity : DapkActivity() {

    private val viewModel by viewModel { module<SettingsModule>().eventLogViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmallTalkTheme {
                Surface(Modifier.fillMaxSize()) {
                    EventLogScreen(viewModel)
                }
            }
        }
    }
}
