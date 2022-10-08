package app.dapk.st.domain.application.message

import app.dapk.st.core.CachedPreferences
import app.dapk.st.core.readBoolean
import app.dapk.st.core.store

private const val KEY_READ_RECEIPTS_DISABLED = "key_read_receipts_disabled"

class MessageOptionsStore(private val cachedPreferences: CachedPreferences) {

    suspend fun isReadReceiptsDisabled() = cachedPreferences.readBoolean(KEY_READ_RECEIPTS_DISABLED, defaultValue = true)

    suspend fun setReadReceiptsDisabled(isDisabled: Boolean) {
        cachedPreferences.store(KEY_READ_RECEIPTS_DISABLED, isDisabled)
    }

}