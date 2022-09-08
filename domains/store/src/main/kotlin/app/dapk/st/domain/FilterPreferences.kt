package app.dapk.st.domain

import app.dapk.st.core.Preferences
import app.dapk.st.matrix.sync.FilterStore

internal class FilterPreferences(
    private val preferences: Preferences
) : FilterStore {

    override suspend fun store(key: String, filterId: String) {
        preferences.store(key, filterId)
    }

    override suspend fun read(key: String): String? {
        return preferences.readString(key)
    }
}