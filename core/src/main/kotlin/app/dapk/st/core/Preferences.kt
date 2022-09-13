package app.dapk.st.core

interface Preferences {

    suspend fun store(key: String, value: String)
    suspend fun readString(key: String): String?
    suspend fun clear()
    suspend fun remove(key: String)
}

interface CachedPreferences : Preferences

suspend fun Preferences.readBoolean(key: String) = this.readString(key)?.toBooleanStrict()
suspend fun Preferences.store(key: String, value: Boolean) = this.store(key, value.toString())