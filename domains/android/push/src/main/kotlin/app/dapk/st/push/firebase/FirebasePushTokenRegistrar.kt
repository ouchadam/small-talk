package app.dapk.st.push.firebase

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.CrashScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.log
import app.dapk.st.push.PushHandler
import app.dapk.st.push.PushTokenPayload
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPushMessageReceiver
import com.google.firebase.messaging.FirebaseMessaging

private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"

class FirebasePushTokenRegistrar(
    override val errorTracker: ErrorTracker,
    private val context: Context,
    private val pushHandler: PushHandler,
) : PushTokenRegistrar, CrashScope {

    override suspend fun registerCurrentToken() {
        log(AppLogTag.PUSH, "FCM - register current token")
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FirebasePushService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        kotlin.runCatching {
            FirebaseMessaging.getInstance().token().also {
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
        FirebaseMessaging.getInstance().deleteToken()
        context.stopService(Intent(context, FirebasePushService::class.java))

        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FirebasePushService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

}