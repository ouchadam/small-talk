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
        summary: String = "a summary line",
        messageCount: Int = 1,
        isAlerting: Boolean = false,
    ) = NotificationTypes.Room(
        anAndroidNotification(),
        aRoomId(),
        summary = summary,
        messageCount = messageCount,
        isAlerting = isAlerting
    )

    fun aDismissRoomNotification(
        roomId: RoomId = aRoomId()
    ) = NotificationTypes.DismissRoom(roomId)

}