package app.dapk.st.push.unifiedpush

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.st.push.Registrar

class UnifiedPushRegistrar(
    private val context: Context,
    private val unifiedPush: UnifiedPush,
    private val componentFactory: (Context) -> ComponentName = { ComponentName(it, UnifiedPushMessageReceiver::class.java) }
) : PushTokenRegistrar {

    fun registerSelection(registrar: Registrar) {
        log(AppLogTag.PUSH, "UnifiedPush - register: $registrar")
        unifiedPush.saveDistributor(context, registrar.id)
        registerApp()
    }

    override suspend fun registerCurrentToken() {
        log(AppLogTag.PUSH, "UnifiedPush - register current token")
        if (unifiedPush.getDistributor(context).isNotEmpty()) {
            registerApp()
        }
    }

    private fun registerApp() {
        context.packageManager.setComponentEnabledSetting(
            componentFactory(context),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        unifiedPush.registerApp(context)
    }

    override fun unregister() {
        unifiedPush.unregisterApp(context)
        context.packageManager.setComponentEnabledSetting(
            componentFactory(context),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

}
