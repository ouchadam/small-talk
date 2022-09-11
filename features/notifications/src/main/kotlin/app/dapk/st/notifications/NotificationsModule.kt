package app.dapk.st.notifications

import android.app.NotificationManager
import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.sync.OverviewStore
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.navigator.IntentFactory
import java.time.Clock

class NotificationsModule(
    private val iconLoader: IconLoader,
    private val roomStore: RoomStore,
    private val overviewStore: OverviewStore,
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
            observeRenderableUnreadEventsUseCase = ObserveUnreadNotificationsUseCaseImpl(roomStore),
            notificationChannels = NotificationChannels(notificationManager),
            observeInviteNotificationsUseCase = ObserveInviteNotificationsUseCaseImpl(overviewStore),
            inviteRenderer = NotificationInviteRenderer(notificationManager, notificationFactory, androidNotificationBuilder)
        )
    }

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

}
