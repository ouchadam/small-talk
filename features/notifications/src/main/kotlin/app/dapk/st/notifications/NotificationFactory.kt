package app.dapk.st.notifications

import android.app.Notification
import android.content.Context
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.whenPOrHigher
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.navigator.IntentFactory

private const val GROUP_ID = "st"

class NotificationFactory(
    private val context: Context,
    private val notificationStyleFactory: NotificationStyleFactory,
    private val intentFactory: IntentFactory,
    private val iconLoader: IconLoader,
    private val deviceMeta: DeviceMeta,
) {
    private val shouldAlwaysAlertDms = true

    suspend fun createMessageNotification(
        events: List<Notifiable>,
        roomOverview: RoomOverview,
        roomsWithNewEvents: Set<RoomId>,
        newRooms: Set<RoomId>
    ): NotificationTypes {
        val sortedEvents = events.sortedBy { it.utcTimestamp }
        val messageStyle = notificationStyleFactory.message(sortedEvents, roomOverview)
        val openRoomIntent = intentFactory.notificationOpenMessage(context, roomOverview.roomId)
        val shouldAlertMoreThanOnce = when {
            roomOverview.isDm() -> roomsWithNewEvents.contains(roomOverview.roomId) && shouldAlwaysAlertDms
            else -> newRooms.contains(roomOverview.roomId)
        }

        return NotificationTypes.Room(
            AndroidNotification(
                channelId = channelId,
                whenTimestamp = sortedEvents.last().utcTimestamp,
                groupId = GROUP_ID,
                groupAlertBehavior = deviceMeta.whenPOrHigher(
                    block = { Notification.GROUP_ALERT_SUMMARY },
                    fallback = { null }
                ),
                shortcutId = roomOverview.roomId.value,
                alertMoreThanOnce = shouldAlertMoreThanOnce,
                contentIntent = openRoomIntent,
                messageStyle = messageStyle,
                category = Notification.CATEGORY_MESSAGE,
                smallIcon = R.drawable.ic_notification_small_icon,
                largeIcon = roomOverview.roomAvatarUrl?.let { iconLoader.load(it.value) },
                autoCancel = true
            ),
            roomId = roomOverview.roomId,
            summary = sortedEvents.last().content,
            messageCount = sortedEvents.size,
            isAlerting = shouldAlertMoreThanOnce
        )
    }

    fun createSummary(notifications: List<NotificationTypes.Room>): AndroidNotification {
        val summaryInboxStyle = notificationStyleFactory.summary(notifications)
        val openAppIntent = intentFactory.notificationOpenApp(context)
        return AndroidNotification(
            channelId = channelId,
            messageStyle = summaryInboxStyle,
            alertMoreThanOnce = notifications.any { it.isAlerting },
            smallIcon = R.drawable.ic_notification_small_icon,
            contentIntent = openAppIntent,
            groupId = GROUP_ID,
            groupAlertBehavior = deviceMeta.whenPOrHigher(
                block = { Notification.GROUP_ALERT_SUMMARY },
                fallback = { null }
            ),
            isGroupSummary = true,
        )
    }
}

private fun RoomOverview.isDm() = !this.isGroup
