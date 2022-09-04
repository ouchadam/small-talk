package app.dapk.st.verification

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel

class VerificationActivity : DapkActivity() {

    private val verificationViewModel by viewModel { module<VerificationModule>().verificationViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmallTalkTheme {
                Surface(Modifier.fillMaxSize()) {
                    VerificationScreen(verificationViewModel)
                }
            }
        }
    }
}
