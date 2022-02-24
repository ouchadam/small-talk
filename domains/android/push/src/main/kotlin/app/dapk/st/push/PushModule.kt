package app.dapk.st.push

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.push.PushService

class PushModule(
    private val pushService: PushService,
    private val errorTracker: ErrorTracker,
) {

    fun registerFirebasePushTokenUseCase() = RegisterFirebasePushTokenUseCase(
        pushService,
        errorTracker,
    )

}