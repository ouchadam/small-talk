package app.dapk.st.settings

import app.dapk.st.domain.eventlog.LoggingStore
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeLoggingStore {
    val instance = mockk<LoggingStore>()

    fun givenLoggingIsEnabled() = coEvery { instance.isEnabled() }.delegateReturn()
}