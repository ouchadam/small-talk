package app.dapk.st.core

import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.navigator.IntentFactory

class CoreAndroidModule(
    private val intentFactory: IntentFactory,
    private val preferences: Lazy<Preferences>,
) : ProvidableModule {

    fun intentFactory() = intentFactory

    private val themeStore by unsafeLazy { ThemeStore(preferences.value) }

    fun themeStore() = themeStore

}