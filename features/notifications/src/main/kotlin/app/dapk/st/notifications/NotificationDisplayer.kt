package app.dapk.st.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import app.dapk.st.core.extensions.ErrorTracker

class NotificationDisplayer(
    context: Context,
    private val errorTracker: ErrorTracker,
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showNotificationMessage(tag: String?, id: Int, notification: Notification) {
        notificationManager.notify(tag, id, notification)
    }

    fun cancelNotificationMessage(tag: String?, id: Int) {
        notificationManager.cancel(tag, id)
    }

    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            errorTracker.track(e, "Failed to cancel all notifications")
        }
    }
}