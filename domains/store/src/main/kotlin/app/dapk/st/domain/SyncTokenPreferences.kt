package app.dapk.st.domain

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.sync.SyncStore
import app.dapk.st.matrix.sync.SyncStore.SyncKey

internal class SyncTokenPreferences(
    private val preferences: Preferences
) : SyncStore {

    override suspend fun store(key: SyncKey, syncToken: SyncToken) {
        log(AppLogTag.ERROR_NON_FATAL, "Store token :$syncToken")
        preferences.store(key.value, syncToken.value)
    }

    override suspend fun read(key: SyncKey): SyncToken? {
        return preferences.readString(key.value)?.let {
            log(AppLogTag.ERROR_NON_FATAL, "Read token :$it")
            SyncToken(it)
        }
    }

    override suspend fun remove(key: SyncKey) {
        preferences.remove(key.value)
    }
}
