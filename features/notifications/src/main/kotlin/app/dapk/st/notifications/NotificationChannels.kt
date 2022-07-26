package app.dapk.st.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build

const val DIRECT_CHANNEL_ID = "direct_channel_id"
const val GROUP_CHANNEL_ID = "group_channel_id"

private const val CHATS_NOTIFICATION_GROUP_ID = "chats_notification_group"

class NotificationChannels(
    private val notificationManager: NotificationManager
) {

    fun initChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(CHATS_NOTIFICATION_GROUP_ID, "Chats"))

            if (notificationManager.getNotificationChannel(DIRECT_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        DIRECT_CHANNEL_ID,
                        "Direct notifications",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).also {
                        it.enableVibration(true)
                        it.enableLights(true)
                        it.group = CHATS_NOTIFICATION_GROUP_ID
                    }
                )
            }

            if (notificationManager.getNotificationChannel(GROUP_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        GROUP_CHANNEL_ID,
                        "Group notifications",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).also {
                        it.group = CHATS_NOTIFICATION_GROUP_ID
                    }
                )
            }
        }
    }

}