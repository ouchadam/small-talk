package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.auth.DeviceDisplayNameGenerator
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ensureTrailingSlash

private const val MATRIX_DOT_ORG_DOMAIN = "matrix.org"

class LoginWithUserPasswordUseCase(
    private val httpClient: MatrixHttpClient,
    private val credentialsProvider: CredentialsStore,
    private val fetchWellKnownUseCase: FetchWellKnownUseCase,
    private val deviceDisplayNameGenerator: DeviceDisplayNameGenerator,
) {

    suspend fun login(userName: String, password: String): AuthService.LoginResult {
        val (domainUrl, fullUserId) = generateUserAccessInfo(userName)
        return when (val wellKnownResult = fetchWellKnownUseCase(domainUrl)) {
            is WellKnownResult.Success -> {
                runCatching {
                    authenticate(wellKnownResult.wellKnown.homeServer.baseUrl.ensureTrailingSlash(), fullUserId, password)
                }.fold(
                    onSuccess = { AuthService.LoginResult.Success(it) },
                    onFailure = { AuthService.LoginResult.Error(it) }
                )
            }

            WellKnownResult.InvalidWellKnown -> AuthService.LoginResult.MissingWellKnown
            WellKnownResult.MissingWellKnown -> AuthService.LoginResult.MissingWellKnown
            is WellKnownResult.Error -> AuthService.LoginResult.Error(wellKnownResult.cause)
        }
    }

    private fun generateUserAccessInfo(userName: String): Pair<String, UserId> {
        val cleanedUserName = userName.ensureStartsWithAt().trim()
        val domain = cleanedUserName.findDomain(fallback = MATRIX_DOT_ORG_DOMAIN)
        val domainUrl = domain.asHttpsUrl()
        val fullUserId = cleanedUserName.ensureHasDomain(domain)
        return Pair(domainUrl, UserId(fullUserId))
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

    private fun String.findDomain(fallback: String) = this.substringAfter(":", missingDelimiterValue = fallback)

    private fun String.asHttpsUrl(): String {
        return "https://$this".ensureTrailingSlash()
    }
}

private fun HomeServerUrl.ensureTrailingSlash() = HomeServerUrl(this.value.ensureTrailingSlash())

private fun String.ensureHasDomain(domain: String) = if (this.endsWith(domain)) this else "$this:$domain"

private fun String.ensureStartsWithAt(): String {
    return when (this.startsWith("@")) {
        true -> this
        false -> "@$this"
    }
}
