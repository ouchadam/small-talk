package app.dapk.st.firebase.crashlytics

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy

class CrashlyticsModule {

    val errorTracker: ErrorTracker by unsafeLazy {
        object : ErrorTracker {
            override fun track(throwable: Throwable, extra: String) {
                // no op
            }
        }
    }

}