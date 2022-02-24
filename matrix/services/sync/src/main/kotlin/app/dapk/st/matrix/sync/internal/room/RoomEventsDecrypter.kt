package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.DecryptedContent
import kotlinx.serialization.json.Json

internal class RoomEventsDecrypter(
    private val messageDecrypter: MessageDecrypter,
    private val json: Json,
    private val logger: MatrixLogger,
) {

    suspend fun decryptRoomEvents(events: List<RoomEvent>) = events.map { event ->
        when (event) {
            is RoomEvent.Message -> event.decrypt()
            is RoomEvent.Reply -> RoomEvent.Reply(
                message = event.message.decrypt(),
                replyingTo = event.replyingTo.decrypt(),
            )
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
        content = decryptedContent.content.body ?: "",
        encryptedContent = null
    )

}

private fun RoomEvent.Message.MegOlmV1.toModel() = EncryptedMessageContent.MegOlmV1(
    this.cipherText,
    this.deviceId,
    this.senderKey,
    this.sessionId,
)