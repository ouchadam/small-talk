package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.ifOrNull
import app.dapk.st.core.extensions.nullAndTrack
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomMembersService
import app.dapk.st.matrix.sync.find
import app.dapk.st.matrix.sync.internal.request.ApiEncryptedContent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent

private typealias Lookup = suspend (EventId) -> LookupResult

internal class RoomEventCreator(
    private val roomMembersService: RoomMembersService,
    private val errorTracker: ErrorTracker,
    private val roomEventFactory: RoomEventFactory,
) {

    suspend fun ApiTimelineEvent.Encrypted.toRoomEvent(roomId: RoomId): RoomEvent? {
        return when (this.encryptedContent) {
            is ApiEncryptedContent.MegOlmV1 -> {
                RoomEvent.Encrypted(
                    eventId = this.eventId,
                    author = roomMembersService.find(roomId, this.senderId)!!,
                    utcTimestamp = this.utcTimestamp,
                    meta = MessageMeta.FromServer,
                    encryptedContent = RoomEvent.Encrypted.MegOlmV1(
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

    suspend fun ApiTimelineEvent.TimelineMessage.toRoomEvent(userCredentials: UserCredentials, roomId: RoomId, lookup: Lookup): RoomEvent? {
        return TimelineEventMapper(userCredentials, roomId, roomEventFactory).mapToRoomEvent(this, lookup)
    }
}

internal class TimelineEventMapper(
    private val userCredentials: UserCredentials,
    private val roomId: RoomId,
    private val roomEventFactory: RoomEventFactory,
) {

    suspend fun mapToRoomEvent(event: ApiTimelineEvent.TimelineMessage, lookup: Lookup): RoomEvent? {
        return when {
            event.content == ApiTimelineEvent.TimelineMessage.Content.Ignored -> null
            event.isEdit() -> event.handleEdit(editedEventId = event.content.relation!!.eventId!!, lookup)
            event.isReply() -> event.handleReply(replyToId = event.content.relation!!.inReplyTo!!.eventId, lookup)
            else -> roomEventFactory.mapToRoomEvent(event)
        }
    }

    private suspend fun ApiTimelineEvent.TimelineMessage.handleReply(replyToId: EventId, lookup: Lookup): RoomEvent {
        val relationEvent = lookup(replyToId).fold(
            onApiTimelineEvent = { it.toMessage() },
            onRoomEvent = { it },
            onEmpty = { null }
        )

        return when (relationEvent) {
            null -> this.toMessage()
            else -> {
                RoomEvent.Reply(
                    message = roomEventFactory.mapToRoomEvent(this),
                    replyingTo = when (relationEvent) {
                        is RoomEvent.Message -> relationEvent
                        is RoomEvent.Reply -> relationEvent.message
                        is RoomEvent.Image -> relationEvent
                        is RoomEvent.Encrypted -> relationEvent
                    }
                )
            }
        }
    }

    private suspend fun ApiTimelineEvent.TimelineMessage.toMessage() = when (this.content) {
        is ApiTimelineEvent.TimelineMessage.Content.Image -> this.toImageMessage()
        is ApiTimelineEvent.TimelineMessage.Content.Text -> this.toFallbackTextMessage()
        ApiTimelineEvent.TimelineMessage.Content.Ignored -> throw IllegalStateException()
    }

    private suspend fun ApiTimelineEvent.TimelineMessage.toFallbackTextMessage() = this.toTextMessage(content = this.asTextContent().body ?: "redacted")

    private suspend fun ApiTimelineEvent.TimelineMessage.handleEdit(editedEventId: EventId, lookup: Lookup): RoomEvent? {
        return lookup(editedEventId).fold(
            onApiTimelineEvent = { editApiEvent(original = it, incomingEdit = this) },
            onRoomEvent = { editRoomEvent(original = it, incomingEdit = this) },
            onEmpty = { this.toTextMessage(edited = true) }
        )
    }

    private fun editRoomEvent(original: RoomEvent, incomingEdit: ApiTimelineEvent.TimelineMessage): RoomEvent? {
        return ifOrNull(incomingEdit.utcTimestamp > original.utcTimestamp) {
            when (original) {
                is RoomEvent.Message -> original.edited(incomingEdit)
                is RoomEvent.Reply -> original.copy(
                    message = when (original.message) {
                        is RoomEvent.Image -> original.message
                        is RoomEvent.Message -> original.message.edited(incomingEdit)
                        is RoomEvent.Reply -> original.message
                        is RoomEvent.Encrypted -> original.message
                    }
                )

                is RoomEvent.Image -> {
                    // can't edit images
                    null
                }

                is RoomEvent.Encrypted -> {
                    // can't edit encrypted messages
                    null
                }
            }
        }
    }

    private suspend fun editApiEvent(original: ApiTimelineEvent.TimelineMessage, incomingEdit: ApiTimelineEvent.TimelineMessage): RoomEvent? {
        return ifOrNull(incomingEdit.utcTimestamp > original.utcTimestamp) {
            when (original.content) {
                is ApiTimelineEvent.TimelineMessage.Content.Image -> original.toImageMessage(
                    utcTimestamp = incomingEdit.utcTimestamp,
                    edited = true,
                )

                is ApiTimelineEvent.TimelineMessage.Content.Text -> original.toTextMessage(
                    utcTimestamp = incomingEdit.utcTimestamp,
                    content = incomingEdit.asTextContent().body?.removePrefix(" * ")?.trim() ?: "redacted",
                    edited = true,
                )

                ApiTimelineEvent.TimelineMessage.Content.Ignored -> null
            }
        }
    }

    // TODO handle edits
    private fun RoomEvent.Message.edited(edit: ApiTimelineEvent.TimelineMessage) = this.copy(
        utcTimestamp = edit.utcTimestamp,
        edited = true,
    )

    private suspend fun RoomEventFactory.mapToRoomEvent(source: ApiTimelineEvent.TimelineMessage): RoomEvent {
        return when (source.content) {
            is ApiTimelineEvent.TimelineMessage.Content.Image -> source.toImageMessage(userCredentials, roomId)
            is ApiTimelineEvent.TimelineMessage.Content.Text -> source.toTextMessage(roomId, content = source.asTextContent().formattedBody ?: source.content.body ?: "")
            ApiTimelineEvent.TimelineMessage.Content.Ignored -> throw IllegalStateException()
        }
    }

    private suspend fun ApiTimelineEvent.TimelineMessage.toTextMessage(
        content: String = this.asTextContent().formattedBody?.stripTags() ?: this.asTextContent().body ?: "redacted",
        edited: Boolean = false,
        utcTimestamp: Long = this.utcTimestamp,
    ) = with(roomEventFactory) { toTextMessage(roomId, content, edited, utcTimestamp) }

    private suspend fun ApiTimelineEvent.TimelineMessage.toImageMessage(
        edited: Boolean = false,
        utcTimestamp: Long = this.utcTimestamp,
    ) = with(roomEventFactory) { toImageMessage(userCredentials, roomId, edited, utcTimestamp) }

}

private fun ApiTimelineEvent.TimelineMessage.isEdit() = this.content.relation?.relationType == "m.replace" && this.content.relation?.eventId != null
private fun ApiTimelineEvent.TimelineMessage.isReply() = this.content.relation?.inReplyTo != null
private fun ApiTimelineEvent.TimelineMessage.asTextContent() = this.content as ApiTimelineEvent.TimelineMessage.Content.Text
