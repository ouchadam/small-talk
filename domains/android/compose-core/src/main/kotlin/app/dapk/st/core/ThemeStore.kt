package app.dapk.st.core

import kotlinx.coroutines.runBlocking

private const val KEY_MATERIAL_YOU_ENABLED = "material_you_enabled"

class ThemeStore(
    private val preferences: Preferences
) {

    private var _isMaterialYouEnabled: Boolean? = null

    fun isMaterialYouEnabled() = _isMaterialYouEnabled ?: blockingInitialRead()

    private fun blockingInitialRead(): Boolean {
        return runBlocking {
            (preferences.readBoolean(KEY_MATERIAL_YOU_ENABLED) ?: false).also { _isMaterialYouEnabled = it }
        }
    }

    suspend fun storeMaterialYouEnabled(isEnabled: Boolean) {
        _isMaterialYouEnabled = isEnabled
        preferences.store(KEY_MATERIAL_YOU_ENABLED, isEnabled)
    }

}