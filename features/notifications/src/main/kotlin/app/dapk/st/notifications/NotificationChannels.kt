package app.dapk.st.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

private const val channelId = "message"

class NotificationChannels(
    private val notificationManager: NotificationManager
) {

    fun initChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

}