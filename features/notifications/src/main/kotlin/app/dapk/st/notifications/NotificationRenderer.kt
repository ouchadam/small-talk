package app.dapk.st.notifications

import android.app.Notification
import android.app.NotificationManager
import app.dapk.st.core.extensions.ifNull
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview

private const val SUMMARY_NOTIFICATION_ID = 101
private const val MESSAGE_NOTIFICATION_ID = 100

class NotificationRenderer(
    private val notificationManager: NotificationManager,
    private val notificationFactory: NotificationFactory,
) {

    suspend fun render(result: Map<RoomOverview, List<RoomEvent>>, removedRooms: Set<RoomId>) {
        removedRooms.forEach { notificationManager.cancel(it.value, MESSAGE_NOTIFICATION_ID) }
        val notifications = notificationFactory.createNotifications(result)

        notifications.summaryNotification.ifNull {
            notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        }

        notifications.delegates.forEach {
            when (it) {
                is NotificationDelegate.DismissRoom -> notificationManager.cancel(it.roomId.value, MESSAGE_NOTIFICATION_ID)
                is NotificationDelegate.Room -> notificationManager.notify(it.roomId.value, MESSAGE_NOTIFICATION_ID, it.notification)
            }
        }

        notifications.summaryNotification?.let {
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, it)
        }
    }

}

sealed interface NotificationDelegate {
    data class Room(val notification: Notification, val roomId: RoomId, val summary: String) : NotificationDelegate
    data class DismissRoom(val roomId: RoomId) : NotificationDelegate
}