package fake

import android.app.NotificationManager
import io.mockk.mockk
import io.mockk.verify

class FakeNotificationManager {

    val instance = mockk<NotificationManager>()

    fun verifyCancelled(tag: String, id: Int) {
        verify { instance.cancel(tag, id) }
    }
}