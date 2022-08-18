package app.dapk.st.push.firebase

import android.content.Context
import android.content.Intent
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.CrashScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log
import app.dapk.st.matrix.push.PushService
import app.dapk.st.push.PushTokenRegistrar
import com.google.firebase.messaging.FirebaseMessaging

internal class RegisterFirebasePushTokenUseCase(
    private val pushService: PushService,
    override val errorTracker: ErrorTracker,
    private val context: Context,
) : PushTokenRegistrar, CrashScope {

    override suspend fun registerCurrentToken() {
        kotlin.runCatching {
            FirebaseMessaging.getInstance().token().also {
                pushService.registerPush(it)
            }
        }
            .trackFailure()
            .onSuccess {
                log(AppLogTag.PUSH, "registered new push token")
            }
    }

    override fun unregister() {
        FirebaseMessaging.getInstance().deleteToken()
        context.stopService(Intent(context, FirebasePushService::class.java))
    }

}