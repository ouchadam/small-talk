package fake

import android.app.Notification
import io.mockk.mockk

class FakeNotificationBuilder {
    val instance = mockk<Notification.Builder>(relaxed = true)
}