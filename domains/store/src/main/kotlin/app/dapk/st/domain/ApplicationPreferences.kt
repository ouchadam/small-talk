package app.dapk.st.domain

import app.dapk.st.core.Preferences

class ApplicationPreferences(
    private val preferences: Preferences,
) {

    suspend fun readVersion(): ApplicationVersion? {
        return preferences.readString("version")?.let { ApplicationVersion(it.toInt()) }
    }

    suspend fun setVersion(version: ApplicationVersion) {
        return preferences.store("version", version.value.toString())
    }

}

data class ApplicationVersion(val value: Int)

