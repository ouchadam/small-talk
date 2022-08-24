package fake

import app.dapk.st.notifications.NotificationState
import app.dapk.st.notifications.NotificationStateMapper
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeNotificationFactory {

    val instance = mockk<NotificationStateMapper>()

    fun givenNotifications(state: NotificationState) = coEvery { instance.mapToNotifications(state) }.delegateReturn()

}