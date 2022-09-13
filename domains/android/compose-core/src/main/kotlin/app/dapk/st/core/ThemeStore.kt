package app.dapk.st.core

private const val KEY_MATERIAL_YOU_ENABLED = "material_you_enabled"

class ThemeStore(
    private val preferences: CachedPreferences
) {

    suspend fun isMaterialYouEnabled() = preferences.readBoolean(KEY_MATERIAL_YOU_ENABLED) ?: false.also { storeMaterialYouEnabled(false) }

    suspend fun storeMaterialYouEnabled(isEnabled: Boolean) {
        preferences.store(KEY_MATERIAL_YOU_ENABLED, isEnabled)
    }

}
