package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService

internal class SendMessageUseCase(
    private val httpClient: MatrixHttpClient,
    private val messageEncrypter: MessageEncrypter,
    private val imageContentReader: ImageContentReader,
) {

    suspend fun sendMessage(message: MessageService.Message): EventId {
        return when (message) {
            is MessageService.Message.TextMessage -> {
                val request = when (message.sendEncrypted) {
                    true -> {
                        sendRequest(
                            roomId = message.roomId,
                            eventType = EventType.ENCRYPTED,
                            txId = message.localId,
                            content = messageEncrypter.encrypt(message),
                        )
                    }

                    false -> {
                        sendRequest(
                            roomId = message.roomId,
                            eventType = EventType.ROOM_MESSAGE,
                            txId = message.localId,
                            content = message.content,
                        )
                    }
                }
                httpClient.execute(request).eventId
            }

            is MessageService.Message.ImageMessage -> {
                println("Sending message")
                val imageContent = imageContentReader.read(message.content.uri)

                println("content: ${imageContent.size}")

                val uri = httpClient.execute(uploadRequest(imageContent.content, imageContent.fileName, "image/png")).contentUri
                println("Got uri $uri")

                val request = sendRequest(
                    roomId = message.roomId,
                    eventType = EventType.ROOM_MESSAGE,
                    txId = message.localId,
                    content = MessageService.Message.Content.ImageContent(
                        url = uri,
                        filename = "foobar.png",
                        MessageService.Message.Content.ImageContent.Info(
                            height = imageContent.height,
                            width = imageContent.width,
                            size = imageContent.size
                        )
                    ),
                )
                httpClient.execute(request).eventId
            }
        }
    }

}


interface ImageContentReader {
    fun read(uri: String): ImageContent

    data class ImageContent(
        val height: Int,
        val width: Int,
        val size: Long,
        val fileName: String,
        val content: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageContent

            if (height != other.height) return false
            if (width != other.width) return false
            if (size != other.size) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = height
            result = 31 * result + width
            result = 31 * result + size.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}