package fake

import app.dapk.st.notifications.NotificationRenderer
import app.dapk.st.notifications.NotificationState
import app.dapk.st.notifications.UnreadNotifications
import io.mockk.coVerify
import io.mockk.mockk

class FakeNotificationRenderer {
    val instance = mockk<NotificationRenderer>()

    fun verifyRenders(vararg unreadNotifications: UnreadNotifications) {
        unreadNotifications.forEach { unread ->
            coVerify {
                instance.render(
                    NotificationState(
                        allUnread = unread.first,
                        removedRooms = unread.second.removed.keys,
                        roomsWithNewEvents = unread.second.changedOrNew.keys,
                        newRooms = unread.second.newRooms,
                    )
                )
            }
        }
    }
}