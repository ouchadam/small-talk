package fixture

import app.dapk.st.engine.*
import app.dapk.st.matrix.common.*

fun aMessengerState(
    self: UserId = aUserId(),
    roomState: RoomState,
    typing: Typing? = null,
    isMuted: Boolean = false,
) = MessengerPageState(self, roomState, typing, isMuted)

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
    content: RichText = RichText.of("encrypted-content"),
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
    message: RoomEvent = aRoomMessageEvent(),
    replyingTo: RoomEvent = aRoomMessageEvent(eventId = anEventId("in-reply-to-id")),
) = RoomEvent.Reply(message, replyingTo)

fun aRoomMessageEvent(
    eventId: EventId = anEventId(),
    utcTimestamp: Long = 0L,
    content: RichText = RichText.of("message-content"),
    author: RoomMember = aRoomMember(),
    meta: MessageMeta = MessageMeta.FromServer,
    edited: Boolean = false,
) = RoomEvent.Message(eventId, utcTimestamp, content, author, meta, edited)

fun anImageMeta(
    width: Int? = 100,
    height: Int? = 100,
    url: String = "https://a-url.com",
    keys: RoomEvent.Image.ImageMeta.Keys? = null
) = RoomEvent.Image.ImageMeta(width, height, url, keys)

fun aRoomState(
    roomOverview: RoomOverview = aRoomOverview(),
    events: List<RoomEvent> = listOf(aRoomMessageEvent()),
) = RoomState(roomOverview, events)

fun aRoomInvite(
    from: RoomMember = aRoomMember(),
    roomId: RoomId = aRoomId(),
    inviteMeta: RoomInvite.InviteMeta = RoomInvite.InviteMeta.DirectMessage,
) = RoomInvite(from, roomId, inviteMeta)

fun aTypingEvent(
    roomId: RoomId = aRoomId(),
    members: List<RoomMember> = listOf(aRoomMember())
) = Typing(roomId, members)