package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.auth.DeviceDisplayNameGenerator
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient

class LoginWithUserPasswordServerUseCase(
    private val httpClient: MatrixHttpClient,
    private val credentialsProvider: CredentialsStore,
    private val deviceDisplayNameGenerator: DeviceDisplayNameGenerator,
) {

    suspend fun login(userName: String, password: String, serverUrl: HomeServerUrl): AuthService.LoginResult {
        return runCatching {
            authenticate(serverUrl, UserId(userName.substringBefore(":")), password)
        }.fold(
            onSuccess = { AuthService.LoginResult.Success(it) },
            onFailure = { AuthService.LoginResult.Error(it) }
        )
    }

    private suspend fun authenticate(baseUrl: HomeServerUrl, fullUserId: UserId, password: String): UserCredentials {
        val authResponse = httpClient.execute(loginRequest(fullUserId, password, baseUrl.value, deviceDisplayNameGenerator.generate()))
        return UserCredentials(
            authResponse.accessToken,
            baseUrl,
            authResponse.userId,
            authResponse.deviceId,
        ).also { credentialsProvider.update(it) }
    }
}
