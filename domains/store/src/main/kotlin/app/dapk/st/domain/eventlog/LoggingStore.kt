package app.dapk.st.domain.eventlog

import app.dapk.st.core.CachedPreferences
import app.dapk.st.core.readBoolean
import app.dapk.st.core.store

private const val KEY_LOGGING_ENABLED = "key_logging_enabled"

class LoggingStore(private val cachedPreferences: CachedPreferences) {

    suspend fun isEnabled() = cachedPreferences.readBoolean(KEY_LOGGING_ENABLED, defaultValue = false)

    suspend fun setEnabled(isEnabled: Boolean) {
        cachedPreferences.store(KEY_LOGGING_ENABLED, isEnabled)
    }

}