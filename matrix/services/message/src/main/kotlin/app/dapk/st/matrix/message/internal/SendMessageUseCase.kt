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
                val imageContent = imageContentReader.read(message.content.uri)
                val uri = httpClient.execute(uploadRequest(imageContent.content, imageContent.fileName, imageContent.mimeType)).contentUri
                val request = sendRequest(
                    roomId = message.roomId,
                    eventType = EventType.ROOM_MESSAGE,
                    txId = message.localId,
                    content = MessageService.Message.Content.ImageContent(
                        url = uri,
                        filename = imageContent.fileName,
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
