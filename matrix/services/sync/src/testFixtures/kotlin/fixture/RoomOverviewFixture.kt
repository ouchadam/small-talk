package fixture

import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.sync.LastMessage
import app.dapk.st.matrix.sync.RoomOverview

fun aMatrixRoomOverview(
    roomId: RoomId = aRoomId(),
    roomCreationUtc: Long = 0L,
    roomName: String? = null,
    roomAvatarUrl: AvatarUrl? = null,
    lastMessage: LastMessage? = null,
    isGroup: Boolean = false,
    readMarker: EventId? = null,
    isEncrypted: Boolean = false,
) = RoomOverview(roomId, roomCreationUtc, roomName, roomAvatarUrl, lastMessage, isGroup, readMarker, isEncrypted)

fun aLastMessage(
    content: String = "last-message-content",
    utcTimestamp: Long = 0L,
    author: RoomMember = aRoomMember(),
) = LastMessage(content, utcTimestamp, author)