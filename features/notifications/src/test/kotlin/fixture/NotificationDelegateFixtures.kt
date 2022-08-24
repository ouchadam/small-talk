package fixture

import android.app.PendingIntent
import android.graphics.drawable.Icon
import app.dapk.st.notifications.AndroidNotification
import app.dapk.st.notifications.AndroidNotificationStyle
import io.mockk.mockk

object NotificationDelegateFixtures {

    fun anAndroidNotification(
        channelId: String = "a channel id",
        whenTimestamp: Long? = 10000,
        isGroupSummary: Boolean = false,
        groupId: String? = "group id",
        groupAlertBehavior: Int? = 5,
        shortcutId: String? = "shortcut id",
        alertMoreThanOnce: Boolean = false,
        contentIntent: PendingIntent? = mockk(),
        messageStyle: AndroidNotificationStyle? = aMessagingStyle(),
        category: String? = "a category",
        smallIcon: Int? = 500,
        largeIcon: Icon? = mockk(),
        autoCancel: Boolean = true,
    ) = AndroidNotification(
        channelId = channelId,
        whenTimestamp = whenTimestamp,
        isGroupSummary = isGroupSummary,
        groupId = groupId,
        groupAlertBehavior = groupAlertBehavior,
        shortcutId = shortcutId,
        alertMoreThanOnce = alertMoreThanOnce,
        contentIntent = contentIntent,
        messageStyle = messageStyle,
        category = category,
        smallIcon = smallIcon,
        largeIcon = largeIcon,
        autoCancel = autoCancel,
    )


    fun aMessagingStyle() = AndroidNotificationStyle.Messaging(
        anAndroidPerson(),
        title = null,
        isGroup = false,
        content = listOf(
            AndroidNotificationStyle.Messaging.AndroidMessage(
                anAndroidPerson(), content = "message content",
                timestamp = 1000
            )
        )
    )

    fun anInboxStyle() = AndroidNotificationStyle.Inbox(
        lines = listOf("first line"),
        summary = null,
    )

    fun anAndroidPerson(
        name: String = "a name",
        key: String = "a unique key",
        icon: Icon? = null,
    ) = AndroidNotificationStyle.Messaging.AndroidPerson(name, key, icon)

}