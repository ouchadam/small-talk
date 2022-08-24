package app.dapk.st.domain.push

import app.dapk.st.domain.Preferences

private const val SELECTION_KEY = "push_token_selection"

class PushTokenRegistrarPreferences(
    private val preferences: Preferences,
) {

    suspend fun currentSelection() = preferences.readString(SELECTION_KEY)

    suspend fun store(registrar: String) {
        preferences.store(SELECTION_KEY, registrar)
    }
}