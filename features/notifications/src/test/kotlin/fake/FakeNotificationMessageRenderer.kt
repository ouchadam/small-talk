package fake

import app.dapk.st.notifications.NotificationMessageRenderer
import app.dapk.st.notifications.NotificationState
import app.dapk.st.engine.UnreadNotifications
import io.mockk.coVerify
import io.mockk.mockk

class FakeNotificationMessageRenderer {
    val instance = mockk<NotificationMessageRenderer>()

    fun verifyRenders(vararg unreadNotifications: app.dapk.st.engine.UnreadNotifications) {
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
