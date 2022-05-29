package app.dapk.st.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import app.dapk.st.core.DapkActivity

class ShareEntryActivity : DapkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val urisToShare = intent.readSendUrisOrNull() ?: throw IllegalArgumentException("")
        
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