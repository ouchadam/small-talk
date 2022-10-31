package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.*
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
        val contents = message.toContents(message.reply)
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
                relatesTo = contents.relatesTo
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
        val imageMeta = message.content.meta
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
                    relatesTo = null
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

private val MX_REPLY_REGEX = "<mx-reply>.*</mx-reply>".toRegex()

class ApiMessageMapper {

    fun Message.TextMessage.toContents(reply: Message.TextMessage.Reply?) = when (reply) {
        null -> ApiMessage.TextMessage.TextContent(
            body = this.content.body.asString(),
        )

        else -> ApiMessage.TextMessage.TextContent(
            body = buildReplyFallback(reply.originalMessage.asString(), reply.author.id, reply.replyContent),
            relatesTo = ApiMessage.RelatesTo(ApiMessage.RelatesTo.InReplyTo(reply.eventId)),
            formattedBody = buildFormattedReply(reply.author.id, reply.originalMessage.asString(), reply.replyContent, this.roomId, reply.eventId),
            format = "org.matrix.custom.html"
        )
    }

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

    private fun buildReplyFallback(originalMessage: String, originalSenderId: UserId, reply: String): String {
        return buildString {
            append("> <")
            append(originalSenderId.value)
            append(">")

            val lines = originalMessage.split("\n")
            lines.forEachIndexed { index, s ->
                if (index == 0) {
                    append(" $s")
                } else {
                    append("\n> $s")
                }
            }
            append("\n\n")
            append(reply)
        }
    }

    private fun buildFormattedReply(userId: UserId, originalMessage: String, reply: String, roomId: RoomId, eventId: EventId): String {
        val permalink = "https://matrix.to/#/${roomId.value}/${eventId.value}"
        val userLink = "https://matrix.to/#/${userId.value}"
        val cleanOriginalMessage = originalMessage.replace(MX_REPLY_REGEX, "")
        return """
            <mx-reply><blockquote><a href="$permalink">In reply to</a> <a href="$userLink">${userId.value}</a><br>${cleanOriginalMessage}</blockquote></mx-reply>$reply
        """.trimIndent()

    }

}
