package app.dapk.st.push

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.CrashScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log
import app.dapk.st.matrix.push.PushService
import com.google.firebase.messaging.FirebaseMessaging

class RegisterFirebasePushTokenUseCase(
    private val pushService: PushService,
    override val errorTracker: ErrorTracker,
) : CrashScope {

    fun unregister() {
        FirebaseMessaging.getInstance().deleteToken()
    }

    suspend fun registerCurrentToken() {
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

}