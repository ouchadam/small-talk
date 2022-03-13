package app.dapk.st.notifications

import android.app.NotificationChannel
import android.app.NotificationManager

private const val channelId = "message"

class NotificationChannels(
    private val notificationManager: NotificationManager
) {

    fun initChannels() {
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

}