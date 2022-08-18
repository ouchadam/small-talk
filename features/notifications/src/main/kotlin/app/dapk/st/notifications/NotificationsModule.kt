package app.dapk.st.notifications

import android.app.NotificationManager
import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.navigator.IntentFactory

class NotificationsModule(
    private val iconLoader: IconLoader,
    private val roomStore: RoomStore,
    private val context: Context,
    private val intentFactory: IntentFactory,
    private val dispatchers: CoroutineDispatchers,
    private val deviceMeta: DeviceMeta,
) : ProvidableModule {

    fun notificationsUseCase() = RenderNotificationsUseCase(
        notificationRenderer = NotificationRenderer(
            notificationManager(),
            NotificationStateMapper(
                RoomEventsToNotifiableMapper(),
                NotificationFactory(
                    context,
                    NotificationStyleFactory(iconLoader, deviceMeta),
                    intentFactory,
                    iconLoader,
                    deviceMeta,
                )
            ),
            AndroidNotificationBuilder(context, deviceMeta, AndroidNotificationStyleBuilder()),
            dispatchers
        ),
        observeRenderableUnreadEventsUseCase = ObserveUnreadNotificationsUseCaseImpl(roomStore),
        notificationChannels = NotificationChannels(notificationManager()),
    )

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

}
