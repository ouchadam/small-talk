package app.dapk.st.matrix.auth

import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.auth.internal.DefaultAuthService
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials

private val SERVICE_KEY = AuthService::class

interface AuthService : MatrixService {
    suspend fun login(userName: String, password: String): UserCredentials
    suspend fun register(userName: String, password: String, homeServer: String): UserCredentials
}

fun MatrixServiceInstaller.installAuthService(
    credentialsStore: CredentialsStore,
    authConfig: AuthConfig = AuthConfig(),
) {
    this.install { (httpClient, json) ->
        SERVICE_KEY to DefaultAuthService(httpClient, credentialsStore, json, authConfig)
    }
}

fun MatrixClient.authService(): AuthService = this.getService(key = SERVICE_KEY)


data class AuthConfig(
    val forceHttp: Boolean = false,
    val forceHomeserver: String? = null
)
