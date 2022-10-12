package app.dapk.st.notifications

import android.app.Notification
import android.content.Context
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.whenPOrHigher
import app.dapk.st.engine.RoomOverview
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.navigator.IntentFactory
import java.time.Clock

private const val GROUP_ID = "st"

class NotificationFactory(
    private val context: Context,
    private val notificationStyleFactory: NotificationStyleFactory,
    private val intentFactory: IntentFactory,
    private val iconLoader: IconLoader,
    private val deviceMeta: DeviceMeta,
    private val clock: Clock,
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

        val last = sortedEvents.last()
        return NotificationTypes.Room(
            AndroidNotification(
                channelId = SUMMARY_CHANNEL_ID,
                whenTimestamp = last.utcTimestamp,
                groupId = GROUP_ID,
                groupAlertBehavior = deviceMeta.whenPOrHigher(
                    block = { Notification.GROUP_ALERT_SUMMARY },
                    fallback = { null }
                ),
                shortcutId = roomOverview.roomId.value,
                alertMoreThanOnce = false,
                contentIntent = openRoomIntent,
                messageStyle = messageStyle,
                category = Notification.CATEGORY_MESSAGE,
                smallIcon = R.drawable.ic_notification_small_icon,
                largeIcon = roomOverview.roomAvatarUrl?.let { iconLoader.load(it.value) },
                autoCancel = true
            ),
            roomId = roomOverview.roomId,
            summary = last.content,
            messageCount = sortedEvents.size,
            isAlerting = shouldAlertMoreThanOnce,
            summaryChannelId = when {
                roomOverview.isDm() -> DIRECT_CHANNEL_ID
                else -> GROUP_CHANNEL_ID
            }
        )
    }

    fun createSummary(notifications: List<NotificationTypes.Room>): AndroidNotification {
        val summaryInboxStyle = notificationStyleFactory.summary(notifications)
        val openAppIntent = intentFactory.notificationOpenApp(context)
        val mostRecent = notifications.mostRecent()
        return AndroidNotification(
            channelId = mostRecent.summaryChannelId,
            messageStyle = summaryInboxStyle,
            whenTimestamp = mostRecent.notification.whenTimestamp,
            alertMoreThanOnce = notifications.any { it.isAlerting },
            smallIcon = R.drawable.ic_notification_small_icon,
            contentIntent = openAppIntent,
            groupId = GROUP_ID,
            groupAlertBehavior = deviceMeta.whenPOrHigher(
                block = { Notification.GROUP_ALERT_SUMMARY },
                fallback = { null }
            ),
            isGroupSummary = true,
            category = Notification.CATEGORY_MESSAGE,
        )
    }

    fun createInvite(inviteNotification: app.dapk.st.engine.InviteNotification): AndroidNotification {
        val openAppIntent = intentFactory.notificationOpenApp(context)
        return AndroidNotification(
            channelId = INVITE_CHANNEL_ID,
            smallIcon = R.drawable.ic_notification_small_icon,
            whenTimestamp = clock.millis(),
            alertMoreThanOnce = true,
            contentTitle = "Invite",
            contentText = inviteNotification.content,
            contentIntent = openAppIntent,
            category = Notification.CATEGORY_EVENT,
            autoCancel = true,
        )
    }
}

private fun List<NotificationTypes.Room>.mostRecent() = this.sortedBy { it.notification.whenTimestamp }.first()

private fun RoomOverview.isDm() = !this.isGroup
