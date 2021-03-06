package app.dapk.st.matrix.room

import app.dapk.st.core.SingletonFlows
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.MatrixServiceProvider
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.internal.DefaultProfileService

private val SERVICE_KEY = ProfileService::class

interface ProfileService : MatrixService {

    suspend fun me(forceRefresh: Boolean): Me

    data class Me(
        val userId: UserId,
        val displayName: String?,
        val avatarUrl: AvatarUrl?,
        val homeServerUrl: HomeServerUrl,
    )

}

fun MatrixServiceInstaller.installProfileService(
    profileStore: ProfileStore,
    singletonFlows: SingletonFlows,
    credentialsStore: CredentialsStore,
) {
    this.install { (httpClient, _, _, logger) ->
        SERVICE_KEY to DefaultProfileService(httpClient, logger, profileStore, singletonFlows, credentialsStore)
    }
}

fun MatrixServiceProvider.profileService(): ProfileService = this.getService(key = SERVICE_KEY)
