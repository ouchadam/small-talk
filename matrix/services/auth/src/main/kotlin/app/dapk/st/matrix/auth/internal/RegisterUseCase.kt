package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.auth.AuthConfig
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ensureTrailingSlash
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RegisterUseCase(
    private val httpClient: MatrixHttpClient,
    private val credentialsProvider: CredentialsStore,
    private val json: Json,
    private val fetchWellKnownUseCase: FetchWellKnownUseCase,
    private val authConfig: AuthConfig,
) {

    suspend fun register(userName: String, password: String, homeServer: String): UserCredentials {
        val baseUrl = homeServer.ifEmpty { "https://${userName.split(":").last()}/" }.ensureTrailingSlash()

        return try {
            httpClient.execute(registerStartFlowRequest(baseUrl))
            throw IllegalStateException("the first request is expected to return a 401")
        } catch (error: ClientRequestException) {
            when (error.response.status.value) {
                401 -> {
                    val stage0 = json.decodeFromString(ApiUserInteractive.serializer(), error.response.bodyAsText())
                    val supportsDummy = stage0.flows.any { it.stages.any { it == "m.login.dummy" } }
                    if (supportsDummy) {
                        registerAccount(userName, password, baseUrl, stage0.session)
                    } else {
                        throw error
                    }
                }
                else -> throw error
            }
        }
    }

    private suspend fun registerAccount(userName: String, password: String, baseUrl: String, session: String): UserCredentials {
        val authResponse = httpClient.execute(
            registerRequest(userName, password, baseUrl, Auth(session, "m.login.dummy"))
        )
        val homeServerUrl = when (authResponse.wellKnown == null) {
            true -> fetchWellKnownUseCase(baseUrl).homeServer.baseUrl
            false -> authResponse.wellKnown.homeServer.baseUrl
        }
        return UserCredentials(
            authResponse.accessToken,
            homeServerUrl,
            authResponse.userId,
            authResponse.deviceId,
        ).also { credentialsProvider.update(it) }
    }
}

@Serializable
internal data class ApiUserInteractive(
    @SerialName("flows") val flows: List<Flow>,
    @SerialName("session") val session: String,
) {
    @Serializable
    data class Flow(
        @SerialName("stages") val stages: List<String>
    )

}