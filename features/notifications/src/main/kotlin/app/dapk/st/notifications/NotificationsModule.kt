package app.dapk.st.notifications

import android.app.NotificationManager
import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.ProvidableModule
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.push.PushService
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.navigator.IntentFactory
import app.dapk.st.push.RegisterFirebasePushTokenUseCase
import app.dapk.st.work.WorkScheduler

class NotificationsModule(
    private val pushService: PushService,
    private val syncService: SyncService,
    private val credentialsStore: CredentialsStore,
    private val firebasePushTokenUseCase: RegisterFirebasePushTokenUseCase,
    private val iconLoader: IconLoader,
    private val roomStore: RoomStore,
    private val context: Context,
    private val workScheduler: WorkScheduler,
    private val intentFactory: IntentFactory,
    private val dispatchers: CoroutineDispatchers,
) : ProvidableModule {

    fun pushUseCase() = pushService
    fun syncService() = syncService
    fun credentialProvider() = credentialsStore
    fun firebasePushTokenUseCase() = firebasePushTokenUseCase
    fun roomStore() = roomStore
    fun notificationsUseCase() = RenderNotificationsUseCase(
        NotificationRenderer(notificationManager(), NotificationFactory(iconLoader, context, intentFactory), dispatchers),
        ObserveUnreadNotificationsUseCaseImpl(roomStore),
        NotificationChannels(notificationManager()),
    )

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun workScheduler() = workScheduler
}
