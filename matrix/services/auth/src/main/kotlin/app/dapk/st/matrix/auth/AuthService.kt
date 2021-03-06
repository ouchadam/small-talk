package app.dapk.st.matrix.auth

import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.auth.internal.DefaultAuthService
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials

private val SERVICE_KEY = AuthService::class

interface AuthService : MatrixService {
    suspend fun login(request: LoginRequest): LoginResult
    suspend fun register(userName: String, password: String, homeServer: String): UserCredentials


    sealed interface LoginResult {
        data class Success(val userCredentials: UserCredentials) : LoginResult
        object MissingWellKnown : LoginResult
        data class Error(val cause: Throwable) : LoginResult
    }

    data class LoginRequest(val userName: String, val password: String, val serverUrl: String?)
}

fun MatrixServiceInstaller.installAuthService(
    credentialsStore: CredentialsStore,
) {
    this.install { (httpClient, json) ->
        SERVICE_KEY to DefaultAuthService(httpClient, credentialsStore, json)
    }
}

fun MatrixClient.authService(): AuthService = this.getService(key = SERVICE_KEY)


data class AuthConfig(
    val forceHttp: Boolean = false,
    val forceHomeserver: String? = null
)
