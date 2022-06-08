package fixture

import android.app.Notification
import app.dapk.st.notifications.NotificationDelegate
import app.dapk.st.notifications.Notifications

object NotificationFixtures {

    fun aNotifications(
        summaryNotification: Notification? = null,
        delegates: List<NotificationDelegate> = emptyList(),
    ) = Notifications(summaryNotification, delegates)

}