package app.dapk.st.matrix.common

interface CredentialsStore {

    suspend fun credentials(): UserCredentials?
    suspend fun update(credentials: UserCredentials)
    suspend fun clear()
}

suspend fun CredentialsStore.isSignedIn() = this.credentials() != null
