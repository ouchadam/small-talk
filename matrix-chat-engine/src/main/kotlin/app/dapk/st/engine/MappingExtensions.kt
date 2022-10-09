package app.dapk.st.engine

import app.dapk.st.matrix.sync.LastMessage as MatrixLastMessage
import app.dapk.st.matrix.sync.RoomOverview as MatrixRoomOverview
import app.dapk.st.matrix.sync.SyncService.SyncEvent.Typing as MatrixTyping

fun MatrixRoomOverview.engine() = RoomOverview(
    this.roomId,
    this.roomCreationUtc,
    this.roomName,
    this.roomAvatarUrl,
    this.lastMessage?.engine(),
    this.isGroup,
    this.readMarker,
    this.isEncrypted
)

fun MatrixLastMessage.engine() = RoomOverview.LastMessage(
    this.content,
    this.utcTimestamp,
    this.author,
)

fun MatrixTyping.engine() = Typing(
    this.roomId,
    this.members,
)