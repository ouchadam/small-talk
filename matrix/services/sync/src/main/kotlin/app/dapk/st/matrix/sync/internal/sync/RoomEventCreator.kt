package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.ifOrNull
import app.dapk.st.core.extensions.nullAndTrack
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.matrixLog
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomMembersService
import app.dapk.st.matrix.sync.find
import app.dapk.st.matrix.sync.internal.request.ApiEncryptedContent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent

private typealias Lookup = suspend (EventId) -> LookupResult

internal class RoomEventCreator(
    private val roomMembersService: RoomMembersService,
    private val logger: MatrixLogger,
    private val errorTracker: ErrorTracker,
) {

    suspend fun ApiTimelineEvent.Encrypted.toRoomEvent(roomId: RoomId): RoomEvent? {
        return when (this.encryptedContent) {
            is ApiEncryptedContent.MegOlmV1 -> {
                RoomEvent.Message(
                    eventId = this.eventId,
                    author = roomMembersService.find(roomId, this.senderId)!!,
                    utcTimestamp = this.utcTimestamp,
                    meta = MessageMeta.FromServer,
                    content = "Encrypted message",
                    encryptedContent = RoomEvent.Message.MegOlmV1(
                        this.encryptedContent.cipherText,
                        this.encryptedContent.deviceId,
                        this.encryptedContent.senderKey,
                        this.encryptedContent.sessionId
                    )
                )
            }
            is ApiEncryptedContent.OlmV1 -> errorTracker.nullAndTrack(IllegalStateException("unexpected encryption, got OlmV1 for a room event"))
            ApiEncryptedContent.Unknown -> errorTracker.nullAndTrack(IllegalStateException("unknown room event encryption"))
        }
    }

    suspend fun ApiTimelineEvent.TimelineText.toRoomEvent(roomId: RoomId, lookup: Lookup): RoomEvent? {
        return when {
            this.isEdit() -> handleEdit(roomId, this.content.relation!!.eventId!!, lookup)
            this.isReply() -> handleReply(roomId, lookup)
            else -> this.toMessage(roomId)
        }
    }

    private suspend fun ApiTimelineEvent.TimelineText.handleEdit(roomId: RoomId, editedEventId: EventId, lookup: Lookup): RoomEvent? {
        return lookup(editedEventId).fold(
            onApiTimelineEvent = {
                ifOrNull(this.utcTimestamp > it.utcTimestamp) {
                    it.toMessage(
                        roomId,
                        utcTimestamp = this.utcTimestamp,
                        content = this.content.body?.removePrefix(" * ")?.trim() ?: "redacted",
                        edited = true,
                    )
                }
            },
            onRoomEvent = {
                ifOrNull(this.utcTimestamp > it.utcTimestamp) {
                    when (it) {
                        is RoomEvent.Message -> it.edited(this)
                        is RoomEvent.Reply -> it.copy(message = it.message.edited(this))
                    }
                }
            },
            onEmpty = { this.toMessage(roomId, edited = true) }
        )
    }

    private fun RoomEvent.Message.edited(edit: ApiTimelineEvent.TimelineText) = this.copy(
        content = edit.content.body?.removePrefix(" * ")?.trim() ?: "redacted",
        utcTimestamp = edit.utcTimestamp,
        edited = true,
    )

    private suspend fun ApiTimelineEvent.TimelineText.handleReply(roomId: RoomId, lookup: Lookup): RoomEvent {
        val replyTo = this.content.relation!!.inReplyTo!!

        val relationEvent = lookup(replyTo.eventId).fold(
            onApiTimelineEvent = { it.toMessage(roomId) },
            onRoomEvent = { it },
            onEmpty = { null }
        )

        logger.matrixLog("found relation: $relationEvent")

        return when (relationEvent) {
            null -> this.toMessage(roomId)
            else -> {
                RoomEvent.Reply(
                    message = this.toMessage(roomId, content = this.content.formattedBody?.stripTags() ?: "redacted"),
                    replyingTo = when (relationEvent) {
                        is RoomEvent.Message -> relationEvent
                        is RoomEvent.Reply -> relationEvent.message
                    }
                )
            }
        }
    }

    private suspend fun ApiTimelineEvent.TimelineText.toMessage(
        roomId: RoomId,
        content: String = this.content.body ?: "redacted",
        edited: Boolean = false,
        utcTimestamp: Long = this.utcTimestamp,
    ) = RoomEvent.Message(
        eventId = this.id,
        content = content,
        author = roomMembersService.find(roomId, this.senderId)!!,
        utcTimestamp = utcTimestamp,
        meta = MessageMeta.FromServer,
        edited = edited,
    )

}

private fun String.stripTags() = this.substring(this.indexOf("</mx-reply>") + "</mx-reply>".length)
    .trim()
    .replace("<em>", "")
    .replace("</em>", "")

private fun ApiTimelineEvent.TimelineText.isEdit() = this.content.relation?.relationType == "m.replace" && this.content.relation.eventId != null
private fun ApiTimelineEvent.TimelineText.isReply() = this.content.relation?.inReplyTo != null