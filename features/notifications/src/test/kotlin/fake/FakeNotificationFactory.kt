package fake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.notifications.NotificationFactory
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeNotificationFactory {

    val instance = mockk<NotificationFactory>()

    fun givenNotifications(allUnread: Map<RoomOverview, List<RoomEvent>>, roomsWithNewEvents: Set<RoomId>, newRooms: Set<RoomId>) = coEvery {
        instance.createNotifications(allUnread, roomsWithNewEvents, newRooms)
    }.delegateReturn()

}