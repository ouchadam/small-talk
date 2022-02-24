package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthConfig
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ensureTrailingSlash

private const val MATRIX_DOT_ORG_DOMAIN = "matrix.org"

class LoginUseCase(
    private val httpClient: MatrixHttpClient,
    private val credentialsProvider: CredentialsStore,
    private val fetchWellKnownUseCase: FetchWellKnownUseCase,
    private val authConfig: AuthConfig
) {

    suspend fun login(userName: String, password: String): UserCredentials {
        val (domainUrl, fullUserId) = generateUserAccessInfo(userName)
        val baseUrl = fetchWellKnownUseCase(domainUrl).homeServer.baseUrl.ensureTrailingSlash()
        val authResponse = httpClient.execute(loginRequest(fullUserId, password, baseUrl.value))
        return UserCredentials(
            authResponse.accessToken,
            baseUrl,
            authResponse.userId,
            authResponse.deviceId,
        ).also { credentialsProvider.update(it) }
    }

    private fun generateUserAccessInfo(userName: String): Pair<String, UserId> {
        val cleanedUserName = userName.ensureStartsWithAt().trim()
        val domain = cleanedUserName.findDomain(fallback = MATRIX_DOT_ORG_DOMAIN)
        val domainUrl = domain.asHttpsUrl()
        val fullUserId = cleanedUserName.ensureHasDomain(domain)
        return Pair(domainUrl, UserId(fullUserId))
    }

    private fun String.findDomain(fallback: String) = this.substringAfter(":", missingDelimiterValue = fallback)

    private fun String.asHttpsUrl(): String {
        val schema = when (authConfig.forceHttp) {
            true -> "http://"
            false -> "https://"
        }
        return "$schema$this".ensureTrailingSlash()
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
