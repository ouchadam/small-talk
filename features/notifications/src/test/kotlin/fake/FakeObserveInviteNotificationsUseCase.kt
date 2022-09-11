package fake

import app.dapk.st.notifications.ObserveInviteNotificationsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateEmit

class FakeObserveInviteNotificationsUseCase : ObserveInviteNotificationsUseCase by mockk() {
    fun given() = coEvery { this@FakeObserveInviteNotificationsUseCase.invoke() }.delegateEmit()
}