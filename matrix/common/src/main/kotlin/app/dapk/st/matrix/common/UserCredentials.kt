package app.dapk.st.matrix.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UserCredentials(
    @SerialName("access_token") val accessToken: String,
    @SerialName("home_server") val homeServer: HomeServerUrl,
    @SerialName("user_id") override val userId: UserId,
    @SerialName("device_id") override val deviceId: DeviceId,
) : DeviceCredentials {

    companion object {

        fun String.fromJson() = Json.decodeFromString(serializer(), this)
        fun UserCredentials.toJson() = Json.encodeToString(serializer(), this)
    }
}

interface DeviceCredentials {
    val userId: UserId
    val deviceId: DeviceId
}
