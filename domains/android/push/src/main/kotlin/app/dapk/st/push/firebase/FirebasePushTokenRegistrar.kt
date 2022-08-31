package app.dapk.st.push.firebase

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.CrashScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log
import app.dapk.st.firebase.messaging.Messaging
import app.dapk.st.push.PushHandler
import app.dapk.st.push.PushTokenPayload
import app.dapk.st.push.PushTokenRegistrar

private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"

class FirebasePushTokenRegistrar(
    override val errorTracker: ErrorTracker,
    private val pushHandler: PushHandler,
    private val messaging: Messaging,
) : PushTokenRegistrar, CrashScope {

    override suspend fun registerCurrentToken() {
        log(AppLogTag.PUSH, "FCM - register current token")
        messaging.enable()

        kotlin.runCatching {
            messaging.token().also {
                pushHandler.onNewToken(
                    PushTokenPayload(
                        token = it,
                        gatewayUrl = SYGNAL_GATEWAY,
                    )
                )
            }
        }
            .trackFailure()
            .onSuccess {
                log(AppLogTag.PUSH, "registered new push token")
            }
    }

    override fun unregister() {
        log(AppLogTag.PUSH, "FCM - unregister")
        messaging.deleteToken()
        messaging.disable()
    }

}