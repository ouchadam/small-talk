package app.dapk.st.push.unifiedpush

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.st.push.Registrar
import org.unifiedpush.android.connector.UnifiedPush

class UnifiedPushRegistrar(
    private val context: Context,
) : PushTokenRegistrar {

    fun registerSelection(registrar: Registrar) {
        log(AppLogTag.PUSH, "UnifiedPush - register: $registrar")
        UnifiedPush.saveDistributor(context, registrar.id)
        registerApp()
    }

    override suspend fun registerCurrentToken() {
        log(AppLogTag.PUSH, "UnifiedPush - register current token")
        if (UnifiedPush.getDistributor(context).isNotEmpty()) {
            registerApp()
        }
    }

    private fun registerApp() {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, UnifiedPushMessageReceiver::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        UnifiedPush.registerApp(context)
    }

    override fun unregister() {
        UnifiedPush.unregisterApp(context)
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, UnifiedPushMessageReceiver::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

}
