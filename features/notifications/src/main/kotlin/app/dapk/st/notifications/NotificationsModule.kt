package app.dapk.st.notifications

import android.app.NotificationManager
import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.engine.ChatEngine
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.navigator.IntentFactory
import java.time.Clock

class NotificationsModule(
    private val chatEngine: ChatEngine,
    private val iconLoader: IconLoader,
    private val context: Context,
    private val intentFactory: IntentFactory,
    private val dispatchers: CoroutineDispatchers,
    private val deviceMeta: DeviceMeta,
) : ProvidableModule {

    fun notificationsUseCase(): RenderNotificationsUseCase {
        val notificationManager = notificationManager()
        val androidNotificationBuilder = AndroidNotificationBuilder(context, deviceMeta, AndroidNotificationStyleBuilder())
        val notificationFactory = NotificationFactory(
            context,
            NotificationStyleFactory(iconLoader, deviceMeta),
            intentFactory,
            iconLoader,
            deviceMeta,
            Clock.systemUTC(),
        )
        val notificationMessageRenderer = NotificationMessageRenderer(
            notificationManager,
            NotificationStateMapper(RoomEventsToNotifiableMapper(), notificationFactory),
            androidNotificationBuilder,
            dispatchers
        )
        return RenderNotificationsUseCase(
            notificationRenderer = notificationMessageRenderer,
            notificationChannels = NotificationChannels(notificationManager),
            inviteRenderer = NotificationInviteRenderer(notificationManager, notificationFactory, androidNotificationBuilder),
            chatEngine = chatEngine,
        )
    }

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

}
