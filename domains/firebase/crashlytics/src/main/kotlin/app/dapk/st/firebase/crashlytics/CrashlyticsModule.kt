package app.dapk.st.firebase.crashlytics

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsModule {

    val errorTracker: ErrorTracker by unsafeLazy {
        CrashlyticsCrashTracker(FirebaseCrashlytics.getInstance())
    }

}