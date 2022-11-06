package app.dapk.st.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build

const val DIRECT_CHANNEL_ID = "direct_channel_id"
const val GROUP_CHANNEL_ID = "group_channel_id"
const val SUMMARY_CHANNEL_ID = "summary_channel_id"
const val INVITE_CHANNEL_ID = "invite_channel_id"

private const val CHATS_NOTIFICATION_GROUP_ID = "chats_notification_group"

class NotificationChannels(
    private val notificationManager: NotificationManager
) {

    fun initChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(CHATS_NOTIFICATION_GROUP_ID, "Chats"))

            notificationManager.createIfMissing(DIRECT_CHANNEL_ID) {
                createChannel(it, "Direct notifications", NotificationManager.IMPORTANCE_HIGH, CHATS_NOTIFICATION_GROUP_ID)
            }
            notificationManager.createIfMissing(GROUP_CHANNEL_ID) {
                createChannel(it, "Group notifications", NotificationManager.IMPORTANCE_HIGH, CHATS_NOTIFICATION_GROUP_ID)
            }
            notificationManager.createIfMissing(INVITE_CHANNEL_ID) {
                createChannel(it, "Invite notifications", NotificationManager.IMPORTANCE_DEFAULT, CHATS_NOTIFICATION_GROUP_ID)
            }
            notificationManager.createIfMissing(SUMMARY_CHANNEL_ID) {
                createChannel(it, "Other notifications", NotificationManager.IMPORTANCE_DEFAULT, CHATS_NOTIFICATION_GROUP_ID)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun createChannel(id: String, name: String, importance: Int, groupId: String) = NotificationChannel(id, name, importance)
        .also { it.group = groupId }

    @SuppressLint("NewApi")
    private fun NotificationManager.createIfMissing(id: String, creator: (String) -> NotificationChannel) {
        if (this.getNotificationChannel(SUMMARY_CHANNEL_ID) == null) {
            this.createNotificationChannel(creator.invoke(id))
        }
    }

}