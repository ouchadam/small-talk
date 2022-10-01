package app.dapk.st.matrix.room

import app.dapk.st.core.SingletonFlows
import app.dapk.st.matrix.InstallExtender
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.MatrixServiceProvider
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.internal.DefaultProfileService
import app.dapk.st.matrix.room.internal.FetchMeUseCase

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
): InstallExtender<ProfileService> {
    return this.install { (httpClient, _, _, _) ->
        val fetchMeUseCase = FetchMeUseCase(httpClient, credentialsStore)
        SERVICE_KEY to DefaultProfileService(profileStore, singletonFlows, fetchMeUseCase)
    }
}

fun MatrixServiceProvider.profileService(): ProfileService = this.getService(key = SERVICE_KEY)
