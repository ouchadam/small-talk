package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthConfig
import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.http.MatrixHttpClient
import kotlinx.serialization.json.Json

internal class DefaultAuthService(
    httpClient: MatrixHttpClient,
    credentialsStore: CredentialsStore,
    json: Json,
    authConfig: AuthConfig,
) : AuthService {

    private val fetchWellKnownUseCase = FetchWellKnownUseCaseImpl(httpClient, json)
    private val loginUseCase = LoginUseCase(httpClient, credentialsStore, fetchWellKnownUseCase, authConfig)
    private val registerCase = RegisterUseCase(httpClient, credentialsStore, json, fetchWellKnownUseCase, authConfig)

    override suspend fun login(userName: String, password: String): UserCredentials {
        return loginUseCase.login(userName, password)
    }

    override suspend fun register(userName: String, password: String, homeServer: String): UserCredentials {
        return registerCase.register(userName, password, homeServer)
    }

}