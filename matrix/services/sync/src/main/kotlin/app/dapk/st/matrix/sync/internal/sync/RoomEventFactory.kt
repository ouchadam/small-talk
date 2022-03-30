package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.convertMxUrToUrl
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomMembersService
import app.dapk.st.matrix.sync.find
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent

internal class RoomEventFactory(
    private val roomMembersService: RoomMembersService
) {

    suspend fun ApiTimelineEvent.TimelineMessage.toTextMessage(
        roomId: RoomId,
        content: String = this.asTextContent().formattedBody?.stripTags() ?: this.asTextContent().body ?: "redacted",
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

    suspend fun ApiTimelineEvent.TimelineMessage.toImageMessage(
        userCredentials: UserCredentials,
        roomId: RoomId,
        edited: Boolean = false,
        utcTimestamp: Long = this.utcTimestamp,
        imageMeta: RoomEvent.Image.ImageMeta = this.readImageMeta(userCredentials)
    ) = RoomEvent.Image(
        eventId = this.id,
        imageMeta = imageMeta,
        author = roomMembersService.find(roomId, this.senderId)!!,
        utcTimestamp = utcTimestamp,
        meta = MessageMeta.FromServer,
        edited = edited,
    )

    private fun ApiTimelineEvent.TimelineMessage.readImageMeta(userCredentials: UserCredentials): RoomEvent.Image.ImageMeta {
        val content = this.content as ApiTimelineEvent.TimelineMessage.Content.Image
        return RoomEvent.Image.ImageMeta(
            content.info.width,
            content.info.height,
            content.file?.url?.convertMxUrToUrl(userCredentials.homeServer)
        )
    }
}

private fun String.stripTags() = this.substring(this.indexOf("</mx-reply>") + "</mx-reply>".length)
    .trim()
    .replace("<em>", "")
    .replace("</em>", "")

private fun ApiTimelineEvent.TimelineMessage.asTextContent() = this.content as ApiTimelineEvent.TimelineMessage.Content.Text
