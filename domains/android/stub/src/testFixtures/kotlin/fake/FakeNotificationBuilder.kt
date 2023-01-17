package fake

import android.app.Notification
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

class FakeNotificationBuilder {
    val instance = mockk<Notification.Builder>(relaxed = true)

    fun givenBuilds() = every { instance.build() }.delegateReturn()
}