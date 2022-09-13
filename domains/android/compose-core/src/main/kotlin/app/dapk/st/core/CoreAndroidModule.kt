package app.dapk.st.core

import app.dapk.st.navigator.IntentFactory

class CoreAndroidModule(
    private val intentFactory: IntentFactory,
    private val preferences: Lazy<CachedPreferences>,
) : ProvidableModule {

    fun intentFactory() = intentFactory

    fun themeStore() = ThemeStore(preferences.value)

}