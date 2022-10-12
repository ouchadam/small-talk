package fake

import app.dapk.st.engine.MetaMapper
import app.dapk.st.matrix.message.MessageService
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

internal class FakeMetaMapper {
    val instance = mockk<MetaMapper>()
    fun given(echo: MessageService.LocalEcho) = every { instance.toMeta(echo) }.delegateReturn()
}