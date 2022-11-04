package fixture

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent

fun aMatrixRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: RichText = RichText.of("message-content"),
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    edited: Boolean = false,
) = RoomEvent.Message(eventId, utcTimestamp, content, author, meta, edited)

fun aRoomImageMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: RoomEvent.Image.ImageMeta = anImageMeta(),
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    edited: Boolean = false,
) = RoomEvent.Image(eventId, utcTimestamp, content, author, meta, edited)

fun aRoomReplyMessageEvent(
    message: RoomEvent = aMatrixRoomMessageEvent(),
    replyingTo: RoomEvent = aMatrixRoomMessageEvent(eventId = anEventId("in-reply-to-id")),
) = RoomEvent.Reply(message, replyingTo)

fun anEncryptedRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    encryptedContent: RoomEvent.Encrypted.MegOlmV1 = aMegolmV1(),
    edited: Boolean = false,
) = RoomEvent.Encrypted(eventId, utcTimestamp, author, meta, edited, encryptedContent)

fun aMegolmV1(
    cipherText: CipherText = CipherText("a-cipher"),
    deviceId: DeviceId = aDeviceId(),
    senderKey: String = "a-sender-key",
    sessionId: SessionId = aSessionId(),
) = RoomEvent.Encrypted.MegOlmV1(cipherText, deviceId, senderKey, sessionId)

fun anImageMeta(
    width: Int? = 100,
    height: Int? = 100,
    url: String = "https://a-url.com",
    keys: RoomEvent.Image.ImageMeta.Keys? = null
) = RoomEvent.Image.ImageMeta(width, height, url, keys)