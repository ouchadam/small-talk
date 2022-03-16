package app.dapk.st.matrix.sync

import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias OverviewState = List<RoomOverview>
typealias InviteState = List<RoomInvite>

@Serializable
data class RoomOverview(
    @SerialName("room_id") val roomId: RoomId,
    @SerialName("room_creation_utc") val roomCreationUtc: Long,
    @SerialName("room_name") val roomName: String?,
    @SerialName("room_avatar") val roomAvatarUrl: AvatarUrl?,
    @SerialName("last_message") val lastMessage: LastMessage?,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("fully_read_marker") val readMarker: EventId?,
    @SerialName("is_encrypted") val isEncrypted: Boolean,
)

@Serializable
data class LastMessage(
    @SerialName("content") val content: String,
    @SerialName("timestamp") val utcTimestamp: Long,
    @SerialName("author") val author: RoomMember,
)

@Serializable
data class RoomInvite(
    @SerialName("from") val from: RoomMember,
    @SerialName("room_id") val roomId: RoomId,
    @SerialName("meta") val inviteMeta: InviteMeta,
)

@Serializable
sealed class InviteMeta {
    @Serializable
    @SerialName("direct_message")
    object DirectMessage : InviteMeta()

    @Serializable
    @SerialName("room")
    data class Room(val roomName: String? = null) : InviteMeta()
}
