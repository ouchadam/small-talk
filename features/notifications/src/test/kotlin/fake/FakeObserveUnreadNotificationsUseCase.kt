package fake

import app.dapk.st.engine.ObserveUnreadNotificationsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateEmit

class FakeObserveUnreadNotificationsUseCase : app.dapk.st.engine.ObserveUnreadNotificationsUseCase by mockk() {
    fun given() = coEvery { this@FakeObserveUnreadNotificationsUseCase.invoke() }.delegateEmit()
}