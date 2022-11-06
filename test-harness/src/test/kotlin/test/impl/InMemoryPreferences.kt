package test.impl

import app.dapk.st.core.Preferences

class InMemoryPreferences : Preferences {

    private val prefs = mutableMapOf<String, String>()

    override suspend fun store(key: String, value: String) {
        prefs[key] = value
    }

    override suspend fun readString(key: String): String? = prefs[key]

    override suspend fun remove(key: String) {
        prefs.remove(key)
    }

    override suspend fun clear() {
        prefs.clear()
    }

}