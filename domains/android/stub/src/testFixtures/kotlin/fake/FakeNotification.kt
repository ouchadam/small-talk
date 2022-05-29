package fake

import android.app.Notification
import io.mockk.mockk

class FakeNotification {

    val instance = mockk<Notification>()

}

fun aFakeNotification() = FakeNotification().instance