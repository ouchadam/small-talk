package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.EventType
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.message.MessageService

internal class SendEventMessageUseCase(
    private val httpClient: MatrixHttpClient,
) {

    suspend fun sendMessage(roomId: RoomId, message: MessageService.EventMessage): EventId {
        return when (message) {
            is MessageService.EventMessage.Encryption -> {
                httpClient.execute(
                    sendRequest(
                        roomId = roomId,
                        eventType = EventType.ENCRYPTION,
                        content = message,
                    )
                ).eventId
            }
        }
    }

}
