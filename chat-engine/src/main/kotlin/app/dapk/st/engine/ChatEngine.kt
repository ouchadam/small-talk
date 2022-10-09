package app.dapk.st.engine

import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import kotlinx.coroutines.flow.Flow

interface ChatEngine {

    fun directory(): Flow<DirectoryState>

}

typealias DirectoryState = List<DirectoryItem>
typealias OverviewState = List<RoomOverview>

data class DirectoryItem(
    val overview: RoomOverview,
    val unreadCount: UnreadCount,
    val typing: Typing?
)

data class RoomOverview(
    val roomId: RoomId,
    val roomCreationUtc: Long,
    val roomName: String?,
    val roomAvatarUrl: AvatarUrl?,
    val lastMessage: LastMessage?,
    val isGroup: Boolean,
    val readMarker: EventId?,
    val isEncrypted: Boolean,
) {

    data class LastMessage(
        val content: String,
        val utcTimestamp: Long,
        val author: RoomMember,
    )

}

@JvmInline
value class UnreadCount(val value: Int)

data class Typing(val roomId: RoomId, val members: List<RoomMember>)
