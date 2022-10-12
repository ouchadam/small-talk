package app.dapk.st.notifications

import android.app.NotificationManager

private const val INVITE_NOTIFICATION_ID = 103

class NotificationInviteRenderer(
    private val notificationManager: NotificationManager,
    private val notificationFactory: NotificationFactory,
    private val androidNotificationBuilder: AndroidNotificationBuilder,
) {

    fun render(inviteNotification: app.dapk.st.engine.InviteNotification) {
        notificationManager.notify(
            inviteNotification.roomId.value,
            INVITE_NOTIFICATION_ID,
            inviteNotification.toAndroidNotification()
        )
    }

    private fun app.dapk.st.engine.InviteNotification.toAndroidNotification() = androidNotificationBuilder.build(
        notificationFactory.createInvite(this)
    )

}