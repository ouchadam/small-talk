package app.dapk.st.domain

import app.dapk.st.core.Preferences
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials

internal class CredentialsPreferences(
    private val preferences: Preferences,
) : CredentialsStore {

    override suspend fun credentials(): UserCredentials? {
        return preferences.readString("credentials")?.let { json ->
            with(UserCredentials) { json.fromJson() }
        }
    }

    override suspend fun update(credentials: UserCredentials) {
        val json = with(UserCredentials) { credentials.toJson() }
        preferences.store("credentials", json)
    }

    override suspend fun clear() {
        preferences.clear()
    }
}
