package app.dapk.st.settings

import app.dapk.st.core.ThemeStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

class FakeThemeStore {
    val instance = mockk<ThemeStore>()

    fun givenMaterialYouIsEnabled() = coEvery { instance.isMaterialYouEnabled() }.delegateReturn()
}