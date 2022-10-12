package fake

import app.dapk.st.engine.DirectoryUseCase
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

internal class FakeDirectoryUseCase {
    val instance = mockk<DirectoryUseCase>()
    fun given() = every { instance.state() }.delegateReturn()
}