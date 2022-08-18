package app.dapk.st.push

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.push.PushService
import app.dapk.st.push.firebase.RegisterFirebasePushTokenUseCase

class PushModule(
    private val pushService: PushService,
    private val errorTracker: ErrorTracker,
    private val pushHandler: PushHandler,
    private val context: Context,
) : ProvidableModule {

    fun pushTokenRegistrar(): PushTokenRegistrar = RegisterFirebasePushTokenUseCase(
        pushService,
        errorTracker,
        context,
    )

    fun pushHandler() = pushHandler

}