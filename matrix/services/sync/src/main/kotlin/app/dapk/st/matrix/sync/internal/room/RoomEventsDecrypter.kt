package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.request.DecryptedContent
import kotlinx.serialization.json.Json

internal class RoomEventsDecrypter(
    private val messageDecrypter: MessageDecrypter,
    private val json: Json,
    private val logger: MatrixLogger,
) {

    suspend fun decryptRoomEvents(userCredentials: UserCredentials, events: List<RoomEvent>) = events.map { event ->
        decryptEvent(event, userCredentials)
    }

    private suspend fun decryptEvent(event: RoomEvent, userCredentials: UserCredentials): RoomEvent = when (event) {
        is RoomEvent.Message -> event.decrypt()
        is RoomEvent.Reply -> RoomEvent.Reply(
            message = decryptEvent(event.message, userCredentials),
            replyingTo = decryptEvent(event.replyingTo, userCredentials),
        )
        is RoomEvent.Image -> event.decrypt(userCredentials)
    }

    private suspend fun RoomEvent.Image.decrypt(userCredentials: UserCredentials) = when (this.encryptedContent) {
        null -> this
        else -> when (val result = messageDecrypter.decrypt(this.encryptedContent.toModel())) {
            is DecryptionResult.Failed -> this.also { logger.crypto("Failed to decrypt ${it.eventId}") }
            is DecryptionResult.Success -> when (val model = result.payload.toModel()) {
                DecryptedContent.Ignored -> this
                is DecryptedContent.TimelineText -> {
                    val content = model.content as ApiTimelineEvent.TimelineMessage.Content.Image
                    this.copy(
                        imageMeta = RoomEvent.Image.ImageMeta(
                            width = content.info.width,
                            height = content.info.height,
                            url = content.file?.url?.convertMxUrToUrl(userCredentials.homeServer) ?: content.url!!.convertMxUrToUrl(userCredentials.homeServer),
                            keys = content.file?.let { RoomEvent.Image.ImageMeta.Keys(it.key.k, it.iv, it.v, it.hashes) }
                        ),
                        encryptedContent = null,
                    )
                }
            }
        }
    }

    private suspend fun RoomEvent.Message.decrypt() = when (this.encryptedContent) {
        null -> this
        else -> when (val result = messageDecrypter.decrypt(this.encryptedContent.toModel())) {
            is DecryptionResult.Failed -> this.also { logger.crypto("Failed to decrypt ${it.eventId}") }
            is DecryptionResult.Success -> when (val model = result.payload.toModel()) {
                DecryptedContent.Ignored -> this
                is DecryptedContent.TimelineText -> this.copyWithDecryptedContent(model)
            }
        }
    }

    private fun JsonString.toModel() = json.decodeFromString(DecryptedContent.serializer(), this.value)

    private fun RoomEvent.Message.copyWithDecryptedContent(decryptedContent: DecryptedContent.TimelineText) = this.copy(
        content = (decryptedContent.content as ApiTimelineEvent.TimelineMessage.Content.Text).body ?: "",
        encryptedContent = null
    )
}

private fun RoomEvent.Message.MegOlmV1.toModel() = EncryptedMessageContent.MegOlmV1(
    this.cipherText,
    this.deviceId,
    this.senderKey,
    this.sessionId,
)