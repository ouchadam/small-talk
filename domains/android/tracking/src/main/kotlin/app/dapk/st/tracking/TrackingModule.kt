package app.dapk.st.tracking

import android.util.Log
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import com.google.firebase.crashlytics.FirebaseCrashlytics

class TrackingModule(
    private val isCrashTrackingEnabled: Boolean,
) {

    val errorTracker: ErrorTracker by unsafeLazy {
        when (isCrashTrackingEnabled) {
            true -> CrashlyticsCrashTracker(FirebaseCrashlytics.getInstance())
            false -> object : ErrorTracker {
                override fun track(throwable: Throwable, extra: String) {
                    Log.e("error", throwable.message, throwable)
                }
            }
        }
    }

}