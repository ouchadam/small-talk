package fixture

import app.dapk.st.engine.*
import app.dapk.st.matrix.common.*

fun aMessengerState(
    self: UserId = aUserId(),
    roomState: RoomState,
    typing: Typing? = null
) = MessengerState(self, roomState, typing)

fun aRoomOverview(
    roomId: RoomId = aRoomId(),
    roomCreationUtc: Long = 0L,
    roomName: String? = null,
    roomAvatarUrl: AvatarUrl? = null,
    lastMessage: RoomOverview.LastMessage? = null,
    isGroup: Boolean = false,
    readMarker: EventId? = null,
    isEncrypted: Boolean = false,
) = RoomOverview(roomId, roomCreationUtc, roomName, roomAvatarUrl, lastMessage, isGroup, readMarker, isEncrypted)

fun anEncryptedRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: String = "encrypted-content",
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    edited: Boolean = false,
    redacted: Boolean = false,
) = RoomEvent.Message(eventId, utcTimestamp, content, author, meta, edited, redacted)
