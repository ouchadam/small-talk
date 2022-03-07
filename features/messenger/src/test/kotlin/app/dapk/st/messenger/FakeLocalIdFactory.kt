package app.dapk.st.messenger

import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

internal class FakeLocalIdFactory {
    val instance = mockk<LocalIdFactory>()
    fun givenCreate() = every { instance.create() }.delegateReturn()
}