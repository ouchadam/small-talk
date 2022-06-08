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
            is MessageService.Message.ImageMessage -> {
                // upload image, then send message
                // POST /_matrix/media/v3/upload
//                message.content.uri

                /**
                 * {
                "content": {
                "body": "filename.jpg",
                "info": {
                "h": 398,
                "mimetype": "image/jpeg",
                "size": 31037,
                "w": 394
                },
                "msgtype": "m.image",
                "url": "mxc://example.org/JWEIFJgwEIhweiWJE"
                },
                "event_id": "$143273582443PhrSn:example.org",
                "origin_server_ts": 1432735824653,
                "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                "sender": "@example:example.org",
                "type": "m.room.message",
                "unsigned": {
                "age": 1234
                }
                }
                 */
                TODO()
            }
        }
    }

}
