package fixture

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.notifications.AndroidNotification
import app.dapk.st.notifications.NotificationTypes
import app.dapk.st.notifications.Notifications
import fixture.NotificationDelegateFixtures.anAndroidNotification

object NotificationFixtures {

    fun aNotifications(
        summaryNotification: AndroidNotification? = null,
        delegates: List<NotificationTypes> = emptyList(),
    ) = Notifications(summaryNotification, delegates)

    fun aRoomNotification(
        notification: AndroidNotification = anAndroidNotification(),
        summary: String = "a summary line",
        messageCount: Int = 1,
        isAlerting: Boolean = false,
        summaryChannelId: String = "a-summary-channel-id",
    ) = NotificationTypes.Room(
        notification,
        aRoomId(),
        summary = summary,
        messageCount = messageCount,
        isAlerting = isAlerting,
        summaryChannelId = summaryChannelId
    )

    fun aDismissRoomNotification(
        roomId: RoomId = aRoomId()
    ) = NotificationTypes.DismissRoom(roomId)

}