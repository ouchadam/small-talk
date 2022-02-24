package fixture

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent

fun aRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: String = "message-content",
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    encryptedContent: RoomEvent.Message.MegOlmV1? = null,
    edited: Boolean = false,
) = RoomEvent.Message(eventId, utcTimestamp, content, author, meta, encryptedContent, edited)

fun aRoomReplyMessageEvent(
    message: RoomEvent.Message = aRoomMessageEvent(),
    replyingTo: RoomEvent.Message = aRoomMessageEvent(eventId = anEventId("in-reply-to-id")),
) = RoomEvent.Reply(message, replyingTo)

fun anEncryptedRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: String = "encrypted-content",
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    encryptedContent: RoomEvent.Message.MegOlmV1? = aMegolmV1(),
    edited: Boolean = false,
) = RoomEvent.Message(eventId, utcTimestamp, content, author, meta, encryptedContent, edited)

fun aMegolmV1(
    cipherText: CipherText = CipherText("a-cipher"),
    deviceId: DeviceId = aDeviceId(),
    senderKey: String = "a-sender-key",
    sessionId: SessionId = aSessionId(),
) = RoomEvent.Message.MegOlmV1(cipherText, deviceId, senderKey, sessionId)