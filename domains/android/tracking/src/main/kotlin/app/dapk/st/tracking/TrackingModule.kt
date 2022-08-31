package app.dapk.st.tracking

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.firebase.crashlytics.CrashlyticsModule

class TrackingModule(
    private val isCrashTrackingEnabled: Boolean,
) {

    val errorTracker: ErrorTracker by unsafeLazy {
        when (isCrashTrackingEnabled) {
            true -> compositeTracker(
                CrashTrackerLogger(),
                CrashlyticsModule().errorTracker,
            )
            false -> CrashTrackerLogger()
        }
    }

}

private fun compositeTracker(vararg loggers: ErrorTracker) = object : ErrorTracker {
    override fun track(throwable: Throwable, extra: String) {
        loggers.forEach { it.track(throwable, extra) }
    }
}