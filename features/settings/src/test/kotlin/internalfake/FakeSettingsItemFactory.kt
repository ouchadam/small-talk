package internalfake

import app.dapk.st.settings.SettingsItemFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

internal class FakeSettingsItemFactory {
    val instance = mockk<SettingsItemFactory>()

    fun givenRoot() = coEvery { instance.root() }.delegateReturn()
}