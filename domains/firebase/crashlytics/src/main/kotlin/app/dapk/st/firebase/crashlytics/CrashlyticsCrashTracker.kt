package app.dapk.st.firebase.crashlytics

import app.dapk.st.core.extensions.ErrorTracker
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsCrashTracker(
    private val firebaseCrashlytics: FirebaseCrashlytics,
) : ErrorTracker {

    override fun track(throwable: Throwable, extra: String) {
        firebaseCrashlytics.recordException(throwable)
    }
}

