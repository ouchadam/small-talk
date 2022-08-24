package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.MxUrl
import app.dapk.st.matrix.common.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiStrippedEvent {

    @Serializable
    @SerialName("m.room.member")
    internal data class RoomMember(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiStrippedEvent() {

        @Serializable
        internal data class Content(
            @SerialName("displayname") val displayName: String? = null,
            @SerialName("membership") val membership: ApiTimelineEvent.RoomMember.Content.Membership? = null,
            @SerialName("is_direct") val isDirect: Boolean? = null,
            @SerialName("avatar_url") val avatarUrl: MxUrl? = null,
        )
    }

    @Serializable
    @SerialName("m.room.name")
    internal data class RoomName(
        @SerialName("content") val content: Content,
    ) : ApiStrippedEvent() {

        @Serializable
        internal data class Content(
            @SerialName("name") val name: String? = null
        )
    }

    @Serializable
    object Ignored : ApiStrippedEvent()
}