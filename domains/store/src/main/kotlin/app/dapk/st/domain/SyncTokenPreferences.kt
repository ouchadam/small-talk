package app.dapk.st.domain

import app.dapk.st.core.Preferences
import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.sync.SyncStore
import app.dapk.st.matrix.sync.SyncStore.SyncKey

internal class SyncTokenPreferences(
    private val preferences: Preferences
) : SyncStore {

    override suspend fun store(key: SyncKey, syncToken: SyncToken) {
        preferences.store(key.value, syncToken.value)
    }

    override suspend fun read(key: SyncKey): SyncToken? {
        return preferences.readString(key.value)?.let {
            SyncToken(it)
        }
    }

    override suspend fun remove(key: SyncKey) {
        preferences.remove(key.value)
    }
}
