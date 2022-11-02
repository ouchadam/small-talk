package app.dapk.st.domain.preference

import app.dapk.st.core.CachedPreferences
import app.dapk.st.core.Preferences

class CachingPreferences(private val cache: PropertyCache, private val preferences: Preferences) : CachedPreferences {

    override suspend fun store(key: String, value: String) {
        cache.setValue(key, value)
        preferences.store(key, value)
    }

    override suspend fun readString(key: String): String? {
        return cache.getValue(key) ?: preferences.readString(key)?.also {
            cache.setValue(key, it)
        }
    }

    override suspend fun readString(key: String, defaultValue: String): String {
        return readString(key) ?: (defaultValue.also { cache.setValue(key, it) })
    }

    override suspend fun remove(key: String) {
        preferences.remove(key)
    }

    override suspend fun clear() {
        preferences.clear()
    }
}