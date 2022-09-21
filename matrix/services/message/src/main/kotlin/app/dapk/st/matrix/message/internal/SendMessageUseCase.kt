package app.dapk.st.matrix.message.internal

import app.dapk.st.core.Base64
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest
import app.dapk.st.matrix.message.ApiSendResponse
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService.Message
import java.io.ByteArrayInputStream

internal class SendMessageUseCase(
    private val httpClient: MatrixHttpClient,
    private val messageEncrypter: MessageEncrypter,
    private val imageContentReader: ImageContentReader,
    private val base64: Base64,
) {

    private val mapper = ApiMessageMapper()

    suspend fun sendMessage(message: Message): EventId {
        return with(mapper) {
            when (message) {
                is Message.TextMessage -> {
                    val request = textMessageRequest(message)
                    httpClient.execute(request).eventId
                }

                is Message.ImageMessage -> {
                    val request = imageMessageRequest(message)
                    httpClient.execute(request).eventId
                }
            }
        }
    }

    private suspend fun ApiMessageMapper.textMessageRequest(message: Message.TextMessage): HttpRequest<ApiSendResponse> {
        val contents = message.toContents()
        return when (message.sendEncrypted) {
            true -> sendRequest(
                roomId = message.roomId,
                eventType = EventType.ENCRYPTED,
                txId = message.localId,
                content = messageEncrypter.encrypt(
                    MessageEncrypter.ClearMessagePayload(
                        message.roomId,
                        contents.toMessageJson(message.roomId)
                    )
                ),
            )

            false -> sendRequest(
                roomId = message.roomId,
                eventType = EventType.ROOM_MESSAGE,
                txId = message.localId,
                content = contents,
            )
        }
    }

    private suspend fun ApiMessageMapper.imageMessageRequest(message: Message.ImageMessage): HttpRequest<ApiSendResponse> {
        val imageContent = imageContentReader.read(message.content.uri)

        return when (message.sendEncrypted) {
            true -> {
                val result = MediaEncrypter(base64).encrypt(
                    ByteArrayInputStream(imageContent.content),
                    imageContent.fileName,
                )

                val uri = httpClient.execute(uploadRequest(result.contents, imageContent.fileName, "application/octet-stream")).contentUri

                val content = ApiMessage.ImageMessage.ImageContent(
                    url = null,
                    filename = imageContent.fileName,
                    file = ApiMessage.ImageMessage.ImageContent.File(
                        url = uri,
                        key = ApiMessage.ImageMessage.ImageContent.File.EncryptionMeta(
                            algorithm = result.algorithm,
                            ext = result.ext,
                            keyOperations = result.keyOperations,
                            kty = result.kty,
                            k = result.k,
                        ),
                        iv = result.iv,
                        hashes = result.hashes,
                        v = result.v,
                    ),
                    info = ApiMessage.ImageMessage.ImageContent.Info(
                        height = imageContent.height,
                        width = imageContent.width,
                        size = imageContent.size
                    )
                )


                val json = JsonString(
                    MatrixHttpClient.jsonWithDefaults.encodeToString(
                        ApiMessage.ImageMessage.serializer(),
                        ApiMessage.ImageMessage(
                            content = content,
                            roomId = message.roomId,
                            type = EventType.ROOM_MESSAGE.value,
                        )
                    )
                )

                sendRequest(
                    roomId = message.roomId,
                    eventType = EventType.ENCRYPTED,
                    txId = message.localId,
                    content = messageEncrypter.encrypt(MessageEncrypter.ClearMessagePayload(message.roomId, json)),
                )
            }

            false -> {
                val uri = httpClient.execute(uploadRequest(imageContent.content, imageContent.fileName, imageContent.mimeType)).contentUri
                sendRequest(
                    roomId = message.roomId,
                    eventType = EventType.ROOM_MESSAGE,
                    txId = message.localId,
                    content = ApiMessage.ImageMessage.ImageContent(
                        url = uri,
                        filename = imageContent.fileName,
                        ApiMessage.ImageMessage.ImageContent.Info(
                            height = imageContent.height,
                            width = imageContent.width,
                            size = imageContent.size
                        )
                    ),
                )
            }
        }
    }

}


class ApiMessageMapper {

    fun Message.TextMessage.toContents() = ApiMessage.TextMessage.TextContent(
        this.content.body,
        this.content.type,
    )

    fun ApiMessage.TextMessage.TextContent.toMessageJson(roomId: RoomId) = JsonString(
        MatrixHttpClient.jsonWithDefaults.encodeToString(
            ApiMessage.TextMessage.serializer(),
            ApiMessage.TextMessage(
                content = this,
                roomId = roomId,
                type = EventType.ROOM_MESSAGE.value
            )
        )
    )

    fun Message.ImageMessage.toContents(uri: MxUrl, image: ImageContentReader.ImageContent) = ApiMessage.ImageMessage.ImageContent(
        url = uri,
        filename = image.fileName,
        ApiMessage.ImageMessage.ImageContent.Info(
            height = image.height,
            width = image.width,
            size = image.size
        )
    )

}
