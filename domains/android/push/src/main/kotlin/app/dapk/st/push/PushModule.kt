package app.dapk.st.push

import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.Preferences
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.domain.push.PushTokenRegistrarPreferences
import app.dapk.st.firebase.messaging.Messaging
import app.dapk.st.push.messaging.MessagingPushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPushImpl
import app.dapk.st.push.unifiedpush.UnifiedPushRegistrar

class PushModule(
    private val errorTracker: ErrorTracker,
    private val pushHandler: PushHandler,
    private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val preferences: Preferences,
    private val messaging: Messaging,
) : ProvidableModule {

    private val registrars by unsafeLazy {
        val unifiedPush = UnifiedPushImpl(context)
        PushTokenRegistrars(
            MessagingPushTokenRegistrar(
                errorTracker,
                pushHandler,
                messaging,
            ),
            UnifiedPushRegistrar(context, unifiedPush),
            PushTokenRegistrarPreferences(preferences),
        )
    }

    fun pushTokenRegistrars() = registrars

    fun pushTokenRegistrar(): PushTokenRegistrar = pushTokenRegistrars()
    fun pushHandler() = pushHandler
    fun dispatcher() = dispatchers

}
