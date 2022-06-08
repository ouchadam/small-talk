package app.dapk.st.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.design.components.SmallTalkTheme

class ShareEntryActivity : DapkActivity() {

    private val viewModel by viewModel { module<ShareEntryModule>().shareEntryViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val urisToShare = intent.readSendUrisOrNull() ?: throw IllegalArgumentException("Expected deeplink uris but they were missing")
        setContent {
            SmallTalkTheme {
                Surface(Modifier.fillMaxSize()) {
                    ShareEntryScreen(navigator, viewModel)
                }
            }
        }
        viewModel.withUris(urisToShare)
    }
}

private fun Intent.readSendUrisOrNull(): List<Uri>? {
    return when (this.action) {
        Intent.ACTION_SEND -> {
            if (this.hasExtra(Intent.EXTRA_STREAM)) {
                listOf(this.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri)
            } else {
                null
            }
        }
        Intent.ACTION_SEND_MULTIPLE -> {
            if (this.hasExtra(Intent.EXTRA_STREAM)) {
                (this.getParcelableArrayExtra(Intent.EXTRA_STREAM) as Array<Uri>).toList()
            } else {
                null
            }
        }
        else -> null
    }
}