package app.dapk.st.matrix.push

import app.dapk.st.matrix.InstallExtender
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.push.internal.DefaultPushService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val SERVICE_KEY = PushService::class

interface PushService : MatrixService {

    suspend fun registerPush(token: String, gatewayUrl: String)

    @Serializable
    data class PushRequest(
        @SerialName("pushkey") val pushKey: String,
        @SerialName("kind") val kind: String?,
        @SerialName("app_id") val appId: String,
        @SerialName("app_display_name") val appDisplayName: String? = null,
        @SerialName("device_display_name") val deviceDisplayName: String? = null,
        @SerialName("profile_tag") val profileTag: String? = null,
        @SerialName("lang") val lang: String? = null,
        @SerialName("data") val data: Payload? = null,
        @SerialName("append") val append: Boolean? = false,
    ) {

        @Serializable
        data class Payload(
            @SerialName("url") val url: String,
            @SerialName("format") val format: String? = null,
            @SerialName("brand") val brand: String? = null,
        )
    }
}

fun MatrixServiceInstaller.installPushService(
    credentialsStore: CredentialsStore,
): InstallExtender<PushService> {
    return this.install { (httpClient, _, _, logger) ->
        SERVICE_KEY to DefaultPushService(httpClient, credentialsStore, logger)
    }
}

fun MatrixClient.pushService(): PushService = this.getService(key = SERVICE_KEY)

