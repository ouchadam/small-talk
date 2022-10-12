package fake

import app.dapk.st.engine.ObserveUnreadNotificationsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateEmit

class FakeObserveUnreadNotificationsUseCase : ObserveUnreadNotificationsUseCase by mockk() {
    fun given() = coEvery { this@FakeObserveUnreadNotificationsUseCase.invoke() }.delegateEmit()
}