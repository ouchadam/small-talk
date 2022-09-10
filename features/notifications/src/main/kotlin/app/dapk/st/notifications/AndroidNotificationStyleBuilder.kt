package app.dapk.st.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.InboxStyle
import android.app.Notification.MessagingStyle
import android.app.Person

@SuppressLint("NewApi")
class AndroidNotificationStyleBuilder(
    private val personBuilderFactory: () -> Person.Builder = { Person.Builder() },
    private val inboxStyleFactory: () -> InboxStyle = { InboxStyle() },
    private val messagingStyleFactory: (Person) -> MessagingStyle = { MessagingStyle(it) },
) {

    fun build(style: AndroidNotificationStyle): Notification.Style {
        return when (style) {
            is AndroidNotificationStyle.Inbox -> style.buildInboxStyle()
            is AndroidNotificationStyle.Messaging -> style.buildMessagingStyle()
        }
    }

    private fun AndroidNotificationStyle.Inbox.buildInboxStyle() = inboxStyleFactory().also { inboxStyle ->
        lines.forEach { inboxStyle.addLine(it) }
        inboxStyle.setSummaryText(summary)
    }

    private fun AndroidNotificationStyle.Messaging.buildMessagingStyle() = messagingStyleFactory(
        personBuilderFactory()
            .setName(person.name)
            .setKey(person.key)
            .build()
    ).also { style ->
        style.conversationTitle = title
        style.isGroupConversation = isGroup
        content.forEach {
            val sender = personBuilderFactory()
                .setName(it.sender.name)
                .setKey(it.sender.key)
                .setIcon(it.sender.icon)
                .build()
            style.addMessage(MessagingStyle.Message(it.content, it.timestamp, sender))
        }
    }

}