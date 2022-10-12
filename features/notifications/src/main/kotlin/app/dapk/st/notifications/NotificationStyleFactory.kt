package app.dapk.st.notifications

import android.annotation.SuppressLint
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.whenPOrHigher
import app.dapk.st.engine.RoomOverview
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.notifications.AndroidNotificationStyle.Inbox
import app.dapk.st.notifications.AndroidNotificationStyle.Messaging

@SuppressLint("NewApi")
class NotificationStyleFactory(
    private val iconLoader: IconLoader,
    private val deviceMeta: DeviceMeta,
) {

    fun summary(notifications: List<NotificationTypes.Room>) = Inbox(
        lines = notifications
            .sortedBy { it.notification.whenTimestamp }
            .map { it.summary },
        summary = "${notifications.countMessages()} messages from ${notifications.size} chats",
    )

    private fun List<NotificationTypes.Room>.countMessages() = this.sumOf { it.messageCount }

    suspend fun message(events: List<Notifiable>, roomOverview: RoomOverview): AndroidNotificationStyle {
        return deviceMeta.whenPOrHigher(
            block = { createMessageStyle(events, roomOverview) },
            fallback = {
                val lines = events.map { "${it.author.displayName ?: it.author.id.value}: ${it.content}" }
                Inbox(lines)
            }
        )
    }

    private suspend fun createMessageStyle(events: List<Notifiable>, roomOverview: RoomOverview) = Messaging(
        Messaging.AndroidPerson(name = "me", key = roomOverview.roomId.value),
        title = roomOverview.roomName.takeIf { roomOverview.isGroup },
        isGroup = roomOverview.isGroup,
        content = events.map { message ->
            Messaging.AndroidMessage(
                Messaging.AndroidPerson(
                    name = message.author.displayName ?: message.author.id.value,
                    icon = message.author.avatarUrl?.let { iconLoader.load(it.value) },
                    key = message.author.id.value,
                ),
                content = message.content,
                timestamp = message.utcTimestamp,
            )
        }
    )

}
