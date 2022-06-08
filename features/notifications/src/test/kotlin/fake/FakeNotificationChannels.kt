package fake

import app.dapk.st.notifications.NotificationChannels
import io.mockk.mockk
import io.mockk.verify

class FakeNotificationChannels {
    val instance = mockk<NotificationChannels>()

    fun verifyInitiated() {
        verify { instance.initChannels() }
    }
}