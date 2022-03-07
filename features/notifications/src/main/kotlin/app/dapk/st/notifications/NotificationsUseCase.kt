package app.dapk.st.notifications

import android.app.*
import android.app.Notification.InboxStyle
import android.content.Context
import app.dapk.st.core.AppLogTag.NOTIFICATION
import app.dapk.st.core.log
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.messenger.MessengerActivity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach

private const val SUMMARY_NOTIFICATION_ID = 101
private const val MESSAGE_NOTIFICATION_ID = 100
private const val GROUP_ID = "st"

class NotificationsUseCase(
    private val roomStore: RoomStore,
    private val notificationManager: NotificationManager,
    private val iconLoader: IconLoader,
    private val context: Context,
) {

    private val inferredCurrentNotifications = mutableSetOf<RoomId>()
    private val channelId = "message"

    init {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "messages",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
    }

    suspend fun listenForNotificationChanges() {
        // TODO handle redactions by removing and edits by not notifying

        roomStore.observeUnread()
            .drop(1)
            .onEach { result ->
                log(NOTIFICATION, "unread changed - render notifications")

                val asRooms = result.keys.map { it.roomId }.toSet()
                val removedRooms = inferredCurrentNotifications - asRooms
                removedRooms.forEach { notificationManager.cancel(it.value, MESSAGE_NOTIFICATION_ID) }

                inferredCurrentNotifications.clear()
                inferredCurrentNotifications.addAll(asRooms)

                val notifications = result.map { (roomOverview, events) ->
                    val messageEvents = events.filterIsInstance<RoomEvent.Message>()
                    when (messageEvents.isEmpty()) {
                        true -> NotificationDelegate.DismissRoom(roomOverview.roomId)
                        false -> createNotification(messageEvents, roomOverview)
                    }
                }


                val summaryNotification = if (notifications.filterIsInstance<NotificationDelegate.Room>().size > 1) {
                    createSummary(notifications)
                } else {
                    null
                }

                if (summaryNotification == null) {
                    notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
                }

                notifications.forEach {
                    when (it) {
                        is NotificationDelegate.DismissRoom -> notificationManager.cancel(it.roomId.value, MESSAGE_NOTIFICATION_ID)
                        is NotificationDelegate.Room -> notificationManager.notify(it.roomId.value, MESSAGE_NOTIFICATION_ID, it.notification)
                    }
                }

                if (summaryNotification != null) {
                    notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
                }

            }
            .collect()
    }

    private fun createSummary(notifications: List<NotificationDelegate>): Notification {
        val summaryInboxStyle = InboxStyle().also { style ->
            notifications.forEach {
                when (it) {
                    is NotificationDelegate.DismissRoom -> {
                        // do nothing
                    }
                    is NotificationDelegate.Room -> style.addLine(it.summary)
                }
            }
        }

        return Notification.Builder(context, channelId)
            .setStyle(summaryInboxStyle)
            .setSmallIcon(R.drawable.ic_notification_small_icon)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroupSummary(true)
            .setGroup(GROUP_ID)
            .build()
    }

    private suspend fun createNotification(events: List<RoomEvent.Message>, roomOverview: RoomOverview): NotificationDelegate {
        val messageStyle = Notification.MessagingStyle(
            Person.Builder()
                .setName("me")
                .setKey(roomOverview.roomId.value)
                .build()
        )

        messageStyle.conversationTitle = roomOverview.roomName.takeIf { roomOverview.isGroup }
        messageStyle.isGroupConversation = roomOverview.isGroup

        events.sortedBy { it.utcTimestamp }.forEach { message ->
            val sender = Person.Builder()
                .setName(message.author.displayName ?: message.author.id.value)
                .setIcon(message.author.avatarUrl?.let { iconLoader.load(it.value) })
                .setKey(message.author.id.value)
                .build()
            messageStyle.addMessage(
                Notification.MessagingStyle.Message(
                    message.content,
                    message.utcTimestamp,
                    sender,
                )
            )
        }

        val openRoomIntent = PendingIntent.getActivity(
            context,
            55,
            MessengerActivity.newInstance(context, roomOverview.roomId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationDelegate.Room(
            Notification.Builder(context, channelId)
                .setWhen(messageStyle.messages.last().timestamp)
                .setShowWhen(true)
                .setGroup(GROUP_ID)
                .setOnlyAlertOnce(roomOverview.isGroup)
                .setContentIntent(openRoomIntent)
                .setStyle(messageStyle)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setShortcutId(roomOverview.roomId.value)
                .setSmallIcon(R.drawable.ic_notification_small_icon)
                .setLargeIcon(roomOverview.roomAvatarUrl?.let { iconLoader.load(it.value) })
                .setAutoCancel(true)
                .build(),
            roomId = roomOverview.roomId,
            summary = messageStyle.messages.last().text.toString()
        )

    }

}

sealed interface NotificationDelegate {

    data class Room(val notification: Notification, val roomId: RoomId, val summary: String) : NotificationDelegate
    data class DismissRoom(val roomId: RoomId) : NotificationDelegate


}