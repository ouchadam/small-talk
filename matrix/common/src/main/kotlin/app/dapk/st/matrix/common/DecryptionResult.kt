package app.dapk.st.matrix.common

sealed interface DecryptionResult {
    data class Failed(val reason: String) : DecryptionResult
    data class Success(val payload: JsonString, val isVerified: Boolean) : DecryptionResult
}
