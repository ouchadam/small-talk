package test.impl

import app.dapk.st.core.Preferences
import test.unit

class InMemoryPreferences : Preferences {

    private val prefs = mutableMapOf<String, String>()

    override suspend fun store(key: String, value: String) {
        prefs[key] = value
    }

    override suspend fun readString(key: String): String? = prefs[key]
    override suspend fun remove(key: String) = prefs.remove(key).unit()
    override suspend fun clear() = prefs.clear()

}