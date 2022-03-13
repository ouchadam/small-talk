package app.dapk.st.matrix.message

import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.MatrixServiceProvider
import app.dapk.st.matrix.ServiceDepFactory
import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.MessageType
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.internal.DefaultMessageService
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private val SERVICE_KEY = MessageService::class

interface MessageService : MatrixService {

    fun localEchos(roomId: RoomId): Flow<List<LocalEcho>>
    fun localEchos(): Flow<Map<RoomId, List<LocalEcho>>>

    suspend fun sendMessage(message: Message)
    suspend fun scheduleMessage(message: Message)
    suspend fun sendEventMessage(roomId: RoomId, message: EventMessage)

    sealed interface EventMessage {

        @Serializable
        data class Encryption(
            @SerialName("algorithm") val algorithm: AlgorithmName
        ) : EventMessage

    }

    @Serializable
    sealed class Message {
        @Serializable
        @SerialName("text_message")
        data class TextMessage(
            @SerialName("content") val content: Content.TextContent,
            @SerialName("send_encrypted") val sendEncrypted: Boolean,
            @SerialName("room_id") val roomId: RoomId,
            @SerialName("local_id") val localId: String,
            @SerialName("timestamp") val timestampUtc: Long,
        ) : Message()

        @Serializable
        sealed class Content {
            @Serializable
            data class TextContent(
                @SerialName("body") val body: String,
                @SerialName("msgtype") val type: String = MessageType.TEXT.value,
            ) : Content()
        }
    }

    @Serializable
    data class LocalEcho(
        @SerialName("event_id") val eventId: EventId?,
        @SerialName("message") val message: Message,
        @SerialName("state") val state: State,
    ) {

        @Transient
        val timestampUtc = when (message) {
            is Message.TextMessage -> message.timestampUtc
        }

        @Transient
        val roomId = when (message) {
            is Message.TextMessage -> message.roomId
        }

        @Transient
        val localId = when (message) {
            is Message.TextMessage -> message.localId
        }

        @Serializable
        sealed class State {
            @Serializable
            @SerialName("sending")
            object Sending : State()

            @Serializable
            @SerialName("sent")
            object Sent : State()

            @Serializable
            @SerialName("error")
            data class Error(
                @SerialName("message") val message: String,
                @SerialName("error_type") val errorType: Type,
            ) : State() {

                @Serializable
                enum class Type {
                    UNKNOWN
                }
            }
        }
    }

}

fun MatrixServiceInstaller.installMessageService(
    localEchoStore: LocalEchoStore,
    backgroundScheduler: BackgroundScheduler,
    messageEncrypter: ServiceDepFactory<MessageEncrypter> = ServiceDepFactory { MissingMessageEncrypter },
) {
    this.install { (httpClient, _, installedServices) ->
        SERVICE_KEY to DefaultMessageService(httpClient, localEchoStore, backgroundScheduler, messageEncrypter.create(installedServices))
    }
}

fun MatrixServiceProvider.messageService(): MessageService = this.getService(key = SERVICE_KEY)
