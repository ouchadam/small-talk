package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService

internal class SendMessageUseCase(
    private val httpClient: MatrixHttpClient,
    private val messageEncrypter: MessageEncrypter,
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
        }
    }

}
