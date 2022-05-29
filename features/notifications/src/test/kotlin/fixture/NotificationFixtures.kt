package app.dapk.st.notifications

import android.app.Notification

object NotificationFixtures {

    fun aNotifications(
        summaryNotification: Notification? = null,
        delegates: List<NotificationDelegate> = emptyList(),
    ) = Notifications(summaryNotification, delegates)

}