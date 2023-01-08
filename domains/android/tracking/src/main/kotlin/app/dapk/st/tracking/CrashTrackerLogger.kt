package app.dapk.st.tracking

import android.util.Log
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log

class CrashTrackerLogger : ErrorTracker {

    override fun track(throwable: Throwable, extra: String) {
        Log.e("ST", throwable.message, throwable)
        log(AppLogTag.ERROR_NON_FATAL, "${throwable.message ?: "N/A"} extra=$extra")

        throwable.findCauseMessage()?.let {
            if (throwable.message != it) {
                log(AppLogTag.ERROR_NON_FATAL, it)
            }
        }
    }
}

private fun Throwable.findCauseMessage(): String? {
    return when (val inner = this.cause) {
        null -> this.message ?: ""
        else -> inner.findCauseMessage()
    }
}

