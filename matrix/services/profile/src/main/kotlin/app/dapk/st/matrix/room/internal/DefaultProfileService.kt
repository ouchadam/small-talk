package app.dapk.st.matrix.room.internal

import app.dapk.st.core.SingletonFlows
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.room.ProfileStore
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class DefaultProfileService(
    private val httpClient: MatrixHttpClient,
    private val logger: MatrixLogger,
    private val profileStore: ProfileStore,
    private val singletonFlows: SingletonFlows,
    private val credentialsStore: CredentialsStore,
) : ProfileService {

    override suspend fun me(forceRefresh: Boolean): ProfileService.Me {
        return when (forceRefresh) {
            true -> fetchMe().also { profileStore.storeMe(it) }
            false -> singletonFlows.getOrPut("me") {
                profileStore.readMe() ?: fetchMe().also { profileStore.storeMe(it) }
            }.first()
        }
    }

    private suspend fun fetchMe(): ProfileService.Me {
        val credentials = credentialsStore.credentials()!!
        val userId = credentials.userId
        return runCatching { httpClient.execute(profileRequest(userId)) }.fold(
            onSuccess = {
                ProfileService.Me(
                    userId,
                    it.displayName,
                    it.avatarUrl?.convertMxUrToUrl(credentials.homeServer)?.let { AvatarUrl(it) },
                    homeServerUrl = credentials.homeServer,
                )
            },
            onFailure = {
                when {
                    it is ClientRequestException && it.response.status.value == 404 -> {
                        ProfileService.Me(
                            userId,
                            displayName = null,
                            avatarUrl = null,
                            homeServerUrl = credentials.homeServer,
                        )
                    }

                    else -> throw it
                }
            }
        )
    }
}

internal fun profileRequest(userId: UserId) = MatrixHttpClient.HttpRequest.httpRequest<ApiMe>(
    path = "_matrix/client/r0/profile/${userId.value}/",
    method = MatrixHttpClient.Method.GET,
)

@Serializable
internal data class ApiMe(
    @SerialName("displayname") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: MxUrl? = null,
)