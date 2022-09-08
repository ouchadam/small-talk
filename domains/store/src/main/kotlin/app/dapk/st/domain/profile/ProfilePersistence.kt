package app.dapk.st.domain.profile

import app.dapk.st.core.Preferences
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.room.ProfileStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class ProfilePersistence(
    private val preferences: Preferences,
) : ProfileStore {

    override suspend fun storeMe(me: ProfileService.Me) {
        preferences.store(
            "me", Json.encodeToString(
                StoreMe.serializer(), StoreMe(
                    userId = me.userId,
                    displayName = me.displayName,
                    avatarUrl = me.avatarUrl,
                    homeServer = me.homeServerUrl,
                )
            )
        )
    }

    override suspend fun readMe(): ProfileService.Me? {
        return preferences.readString("me")?.let {
            Json.decodeFromString(StoreMe.serializer(), it).let {
                ProfileService.Me(
                    userId = it.userId,
                    displayName = it.displayName,
                    avatarUrl = it.avatarUrl,
                    homeServerUrl = it.homeServer
                )
            }
        }
    }

}

@Serializable
private class StoreMe(
    @SerialName("user_id") val userId: UserId,
    @SerialName("display_name") val displayName: String?,
    @SerialName("avatar_url") val avatarUrl: AvatarUrl?,
    @SerialName("homeserver") val homeServer: HomeServerUrl,
)