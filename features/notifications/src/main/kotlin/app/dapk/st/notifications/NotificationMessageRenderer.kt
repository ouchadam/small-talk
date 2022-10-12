package app.dapk.st.notifications

import android.app.NotificationManager
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.extensions.ifNull
import app.dapk.st.core.log
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.RoomOverview
import app.dapk.st.matrix.common.RoomId
import kotlinx.coroutines.withContext

private const val SUMMARY_NOTIFICATION_ID = 101
private const val MESSAGE_NOTIFICATION_ID = 100

class NotificationMessageRenderer(
    private val notificationManager: NotificationManager,
    private val notificationStateMapper: NotificationStateMapper,
    private val androidNotificationBuilder: AndroidNotificationBuilder,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun render(state: NotificationState) {
        state.removedRooms.forEach { notificationManager.cancel(it.value, MESSAGE_NOTIFICATION_ID) }
        val notifications = notificationStateMapper.mapToNotifications(state)

        withContext(dispatchers.main) {
            notifications.summaryNotification.ifNull {
                log(AppLogTag.NOTIFICATION, "cancelling summary")
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
            }

            val onlyContainsRemovals = state.onlyContainsRemovals()
            notifications.delegates.forEach {
                when (it) {
                    is NotificationTypes.DismissRoom -> notificationManager.cancel(it.roomId.value, MESSAGE_NOTIFICATION_ID)
                    is NotificationTypes.Room -> {
                        if (!onlyContainsRemovals) {
                            log(AppLogTag.NOTIFICATION, "notifying ${it.roomId.value}")
                            notificationManager.notify(it.roomId.value, MESSAGE_NOTIFICATION_ID, it.notification.build(androidNotificationBuilder))
                        }
                    }
                }
            }

            notifications.summaryNotification?.let {
                if (notifications.delegates.filterIsInstance<NotificationTypes.Room>().isNotEmpty() && !onlyContainsRemovals) {
                    log(AppLogTag.NOTIFICATION, "notifying summary")
                    notificationManager.notify(SUMMARY_NOTIFICATION_ID, it.build(androidNotificationBuilder))
                }
            }
        }
    }
}

data class NotificationState(
    val allUnread: Map<RoomOverview, List<RoomEvent>>,
    val removedRooms: Set<RoomId>,
    val roomsWithNewEvents: Set<RoomId>,
    val newRooms: Set<RoomId>
)

private fun NotificationState.onlyContainsRemovals() = this.removedRooms.isNotEmpty() && this.roomsWithNewEvents.isEmpty()

sealed interface NotificationTypes {
    data class Room(
        val notification: AndroidNotification,
        val roomId: RoomId,
        val summary: String,
        val messageCount: Int,
        val isAlerting: Boolean,
        val summaryChannelId: String,
    ) : NotificationTypes

    data class DismissRoom(val roomId: RoomId) : NotificationTypes
}