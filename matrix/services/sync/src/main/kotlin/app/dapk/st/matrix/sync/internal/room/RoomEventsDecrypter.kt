package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent.TimelineMessage.Content.Image
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent.TimelineMessage.Content.Text
import app.dapk.st.matrix.sync.internal.request.DecryptedContent
import app.dapk.st.matrix.sync.internal.sync.message.RichMessageParser
import kotlinx.serialization.json.Json

internal class RoomEventsDecrypter(
    private val messageDecrypter: MessageDecrypter,
    private val richMessageParser: RichMessageParser,
    private val json: Json,
    private val logger: MatrixLogger,
) {

    suspend fun decryptRoomEvents(userCredentials: UserCredentials, events: List<RoomEvent>) = events.map { event ->
        decryptEvent(event, userCredentials)
    }

    private suspend fun decryptEvent(event: RoomEvent, userCredentials: UserCredentials): RoomEvent = when (event) {
        is RoomEvent.Encrypted -> event.decrypt(userCredentials)
        is RoomEvent.Message -> event
        is RoomEvent.Reply -> RoomEvent.Reply(
            message = decryptEvent(event.message, userCredentials),
            replyingTo = decryptEvent(event.replyingTo, userCredentials),
        )

        is RoomEvent.Image -> event
        is RoomEvent.Redacted -> event
    }

    private suspend fun RoomEvent.Encrypted.decrypt(userCredentials: UserCredentials) = when (val result = this.decryptContent()) {
        is DecryptionResult.Failed -> this.also { logger.crypto("Failed to decrypt ${it.eventId}") }
        is DecryptionResult.Success -> when (val model = result.payload.toModel()) {
            DecryptedContent.Ignored -> this
            is DecryptedContent.TimelineText -> when (val content = model.content) {
                ApiTimelineEvent.TimelineMessage.Content.Ignored -> this
                is Image -> createImageEvent(content, userCredentials)
                is Text -> createMessageEvent(content)
            }
        }
    }

    private suspend fun RoomEvent.Encrypted.decryptContent() = messageDecrypter.decrypt(this.encryptedContent.toModel())

    private fun RoomEvent.Encrypted.createMessageEvent(content: Text) = RoomEvent.Message(
        eventId = this.eventId,
        utcTimestamp = this.utcTimestamp,
        author = this.author,
        meta = this.meta,
        edited = this.edited,
        content = richMessageParser.parse(content.body ?: "")
    )

    private fun RoomEvent.Encrypted.createImageEvent(content: Image, userCredentials: UserCredentials) = RoomEvent.Image(
        eventId = this.eventId,
        utcTimestamp = this.utcTimestamp,
        author = this.author,
        meta = this.meta,
        edited = this.edited,
        imageMeta = RoomEvent.Image.ImageMeta(
            width = content.info?.width,
            height = content.info?.height,
            url = content.file?.url?.convertMxUrToUrl(userCredentials.homeServer) ?: content.url!!.convertMxUrToUrl(userCredentials.homeServer),
            keys = content.file?.let { RoomEvent.Image.ImageMeta.Keys(it.key.k, it.iv, it.v, it.hashes) }
        ),
    )

    private fun JsonString.toModel() = json.decodeFromString(DecryptedContent.serializer(), this.value)

}

private fun RoomEvent.Encrypted.MegOlmV1.toModel() = EncryptedMessageContent.MegOlmV1(
    this.cipherText,
    this.deviceId,
    this.senderKey,
    this.sessionId,
)