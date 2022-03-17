package app.dapk.st.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.messenger.MessengerActivity

private const val GROUP_ID = "st"
private const val channelId = "message"

class NotificationFactory(
    private val iconLoader: IconLoader,
    private val context: Context,
) {

    suspend fun createNotifications(events: Map<RoomOverview, List<RoomEvent>>): Notifications {
        val notifications = events.map { (roomOverview, events) ->
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
        return Notifications(summaryNotification, notifications)
    }

    private fun createSummary(notifications: List<NotificationDelegate>): Notification {
        val summaryInboxStyle = Notification.InboxStyle().also { style ->
            notifications.forEach {
                when (it) {
                    is NotificationDelegate.DismissRoom -> {
                        // do nothing
                    }
                    is NotificationDelegate.Room -> style.addLine(it.summary)
                }
            }
        }

        return builder()
            .setStyle(summaryInboxStyle)
            .setSmallIcon(R.drawable.ic_notification_small_icon)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroupSummary(true)
            .setGroup(GROUP_ID)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun createMessageStyle(events: List<RoomEvent.Message>, roomOverview: RoomOverview): Notification.MessagingStyle {
        val messageStyle = Notification.MessagingStyle(
            Person.Builder()
                .setName("me")
                .setKey(roomOverview.roomId.value)
                .build()
        )

        messageStyle.conversationTitle = roomOverview.roomName.takeIf { roomOverview.isGroup }
        messageStyle.isGroupConversation = roomOverview.isGroup

        events.forEach { message ->
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
        return messageStyle
    }

    private suspend fun createNotification(events: List<RoomEvent.Message>, roomOverview: RoomOverview): NotificationDelegate {
        val sortedEvents = events.sortedBy { it.utcTimestamp }

        val messageStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            createMessageStyle(sortedEvents, roomOverview)
        } else {
            val inboxStyle = Notification.InboxStyle()
            events.forEach {
                inboxStyle.addLine("${it.author.displayName ?: it.author.id.value}: ${it.content}")
            }
            inboxStyle
        }

        val openRoomIntent = PendingIntent.getActivity(
            context,
            55,
            MessengerActivity.newInstance(context, roomOverview.roomId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationDelegate.Room(
            builder()
                .setWhen(sortedEvents.last().utcTimestamp)
                .setShowWhen(true)
                .setGroup(GROUP_ID)
                .setOnlyAlertOnce(roomOverview.isGroup)
                .setContentIntent(openRoomIntent)
                .setStyle(messageStyle)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        this.setShortcutId(roomOverview.roomId.value)
                    } else {
                        this
                    }
                }
                .setSmallIcon(R.drawable.ic_notification_small_icon)
                .setLargeIcon(roomOverview.roomAvatarUrl?.let { iconLoader.load(it.value) })
                .setAutoCancel(true)
                .build(),
            roomId = roomOverview.roomId,
            summary = events.last().content
        )

    }

    private fun builder() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, channelId)
    } else {
        Notification.Builder(context)
    }

}

data class Notifications(val summaryNotification: Notification?, val delegates: List<NotificationDelegate>)