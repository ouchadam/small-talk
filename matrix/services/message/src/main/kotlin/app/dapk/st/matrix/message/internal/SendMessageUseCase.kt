package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest
import app.dapk.st.matrix.message.ApiSendResponse
import app.dapk.st.matrix.message.MediaEncrypter
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService.Message

internal class SendMessageUseCase(
    private val httpClient: MatrixHttpClient,
    private val messageEncrypter: MessageEncrypter,
    private val mediaEncrypter: MediaEncrypter,
    private val imageContentReader: ImageContentReader,
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

    private suspend fun imageMessageRequest(message: Message.ImageMessage): HttpRequest<ApiSendResponse> {
        val imageMeta = imageContentReader.meta(message.content.uri)

        return when (message.sendEncrypted) {
            true -> {
                val result = mediaEncrypter.encrypt(imageContentReader.inputStream(message.content.uri))
                val uri = httpClient.execute(
                    uploadRequest(
                        result.openStream(),
                        result.contentLength,
                        imageMeta.fileName,
                        "application/octet-stream"
                    )
                ).contentUri

                val content = ApiMessage.ImageMessage.ImageContent(
                    url = null,
                    filename = imageMeta.fileName,
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
                        height = imageMeta.height,
                        width = imageMeta.width,
                        size = imageMeta.size,
                        mimeType = imageMeta.mimeType,
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
                val uri = httpClient.execute(
                    uploadRequest(
                        imageContentReader.inputStream(message.content.uri),
                        imageMeta.size,
                        imageMeta.fileName,
                        imageMeta.mimeType
                    )
                ).contentUri
                sendRequest(
                    roomId = message.roomId,
                    eventType = EventType.ROOM_MESSAGE,
                    txId = message.localId,
                    content = ApiMessage.ImageMessage.ImageContent(
                        url = uri,
                        filename = imageMeta.fileName,
                        ApiMessage.ImageMessage.ImageContent.Info(
                            height = imageMeta.height,
                            width = imageMeta.width,
                            size = imageMeta.size,
                            mimeType = imageMeta.mimeType,
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

}
