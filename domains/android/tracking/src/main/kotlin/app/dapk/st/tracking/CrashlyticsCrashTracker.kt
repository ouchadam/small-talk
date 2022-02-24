package app.dapk.st.tracking

import android.util.Log
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsCrashTracker(
    private val firebaseCrashlytics: FirebaseCrashlytics,
) : ErrorTracker {

    override fun track(throwable: Throwable, extra: String) {
        Log.e("ST", throwable.message, throwable)
        log(AppLogTag.ERROR_NON_FATAL, "${throwable.message ?: "N/A"} extra=$extra")
        firebaseCrashlytics.recordException(throwable)
    }
}

