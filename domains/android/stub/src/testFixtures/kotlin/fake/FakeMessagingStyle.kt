package fake

import android.app.Notification
import android.app.Person
import io.mockk.every
import io.mockk.mockk

class FakeMessagingStyle {
    var user: Person? = null
    val instance = mockk<Notification.MessagingStyle>()

}

fun aFakeMessagingStyle() = FakeMessagingStyle().instance