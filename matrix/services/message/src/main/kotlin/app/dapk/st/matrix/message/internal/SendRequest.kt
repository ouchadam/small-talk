package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.jsonBody
import app.dapk.st.matrix.message.ApiSendResponse
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService.EventMessage
import app.dapk.st.matrix.message.MessageService.Message
import java.util.*

internal fun sendRequest(roomId: RoomId, eventType: EventType, txId: String, content: Message.Content) = httpRequest<ApiSendResponse>(
    path = "_matrix/client/r0/rooms/${roomId.value}/send/${eventType.value}/${txId}",
    method = MatrixHttpClient.Method.PUT,
    body = when (content) {
        is Message.Content.TextContent -> jsonBody(Message.Content.TextContent.serializer(), content, MatrixHttpClient.jsonWithDefaults)
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

fun txId() = "local.${UUID.randomUUID()}"