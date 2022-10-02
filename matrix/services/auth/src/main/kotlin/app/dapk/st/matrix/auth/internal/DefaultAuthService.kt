package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.auth.DeviceDisplayNameGenerator
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ensureHttpsIfMissing
import app.dapk.st.matrix.http.ensureTrailingSlash
import kotlinx.serialization.json.Json

internal class DefaultAuthService(
    httpClient: MatrixHttpClient,
    credentialsStore: CredentialsStore,
    json: Json,
    deviceDisplayNameGenerator: DeviceDisplayNameGenerator,
) : AuthService {

    private val fetchWellKnownUseCase = FetchWellKnownUseCaseImpl(httpClient, json)
    private val loginUseCase = LoginWithUserPasswordUseCase(httpClient, credentialsStore, fetchWellKnownUseCase, deviceDisplayNameGenerator)
    private val loginServerUseCase = LoginWithUserPasswordServerUseCase(httpClient, credentialsStore, deviceDisplayNameGenerator)
    private val registerCase = RegisterUseCase(httpClient, credentialsStore, json, fetchWellKnownUseCase)

    override suspend fun login(request: AuthService.LoginRequest): AuthService.LoginResult {
        return when {
            request.serverUrl == null -> loginUseCase.login(request.userName, request.password)
            else -> {
                val serverUrl = HomeServerUrl(request.serverUrl.ensureHttpsIfMissing().ensureTrailingSlash())
                loginServerUseCase.login(request.userName, request.password, serverUrl)
            }
        }
    }

    override suspend fun register(userName: String, password: String, homeServer: String): UserCredentials {
        return registerCase.register(userName, password, homeServer)
    }

}