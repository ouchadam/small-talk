package app.dapk.st.matrix.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomMember(
    @SerialName("user_id") val id: UserId,
    @SerialName("display_name") val displayName: String?,
    @SerialName("avatar_url") val avatarUrl: AvatarUrl?,
)