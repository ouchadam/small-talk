package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.jsonBody
import app.dapk.st.matrix.message.ApiSendResponse
import app.dapk.st.matrix.message.ApiUploadResponse
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService.EventMessage
import app.dapk.st.matrix.message.internal.ApiMessage.ImageMessage
import app.dapk.st.matrix.message.internal.ApiMessage.TextMessage
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.InputStream
import java.util.*

internal fun sendRequest(roomId: RoomId, eventType: EventType, txId: String, content: ApiMessageContent) = httpRequest<ApiSendResponse>(
    path = "_matrix/client/r0/rooms/${roomId.value}/send/${eventType.value}/${txId}",
    method = MatrixHttpClient.Method.PUT,
    body = when (content) {
        is TextMessage.TextContent -> jsonBody(TextMessage.TextContent.serializer(), content, MatrixHttpClient.jsonWithDefaults)
        is ImageMessage.ImageContent -> jsonBody(ImageMessage.ImageContent.serializer(), content, MatrixHttpClient.jsonWithDefaults)
    }
)

internal fun sendRequest(roomId: RoomId, eventType: EventType, txId: String, content: MessageEncrypter.EncryptedMessagePayload) = httpRequest<ApiSendResponse>(
    path = "_matrix/client/r0/rooms/${roomId.value}/send/${eventType.value}/${txId}",
    method = MatrixHttpClient.Method.PUT,
    body = jsonBody(MessageEncrypter.EncryptedMessagePayload.serializer(), content)
)

internal fun sendRequest(roomId: RoomId, eventType: EventType, content: EventMessage) = httpRequest<ApiSendResponse>(
    path = "_matrix/client/r0/rooms/${roomId.value}/send/${eventType.value}/${txId()}",
    method = MatrixHttpClient.Method.PUT,
    body = when (content) {
        is EventMessage.Encryption -> jsonBody(EventMessage.Encryption.serializer(), content, MatrixHttpClient.jsonWithDefaults)
    }
)

internal fun uploadRequest(stream: InputStream, contentLength: Long, filename: String, contentType: String) = httpRequest<ApiUploadResponse>(
    path = "_matrix/media/r0/upload/?filename=$filename",
    headers = listOf("Content-Type" to contentType),
    method = MatrixHttpClient.Method.POST,
    body = ChannelWriterContent(
        body = { stream.copyTo(this) },
        contentType = ContentType.parse(contentType),
        contentLength = contentLength,
    ),
)

fun txId() = "local.${UUID.randomUUID()}"