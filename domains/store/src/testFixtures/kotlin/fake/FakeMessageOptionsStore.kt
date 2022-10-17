package fake

import app.dapk.st.domain.application.message.MessageOptionsStore
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeMessageOptionsStore {
    val instance = mockk<MessageOptionsStore>()

    fun givenReadReceiptsDisabled() = coEvery { instance.isReadReceiptsDisabled() }.delegateReturn()
}