package fake

import app.dapk.st.notifications.NotificationInviteRenderer
import io.mockk.mockk

class FakeNotificationInviteRenderer {
    val instance = mockk<NotificationInviteRenderer>()
}