package app.dapk.st.push

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.push.PushService

class PushModule(
    private val pushService: PushService,
    private val errorTracker: ErrorTracker,
    private val pushHandler: PushHandler,
) : ProvidableModule {

    fun registerFirebasePushTokenUseCase() = RegisterFirebasePushTokenUseCase(
        pushService,
        errorTracker,
    )

    fun pushHandler() = pushHandler

}