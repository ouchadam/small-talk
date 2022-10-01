package app.dapk.st.matrix.message

import app.dapk.st.matrix.*
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.message.internal.DefaultMessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
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
    sealed interface Message {
        @Serializable
        @SerialName("text_message")
        data class TextMessage(
            @SerialName("content") val content: Content.TextContent,
            @SerialName("send_encrypted") val sendEncrypted: Boolean,
            @SerialName("room_id") val roomId: RoomId,
            @SerialName("local_id") val localId: String,
            @SerialName("timestamp") val timestampUtc: Long,
            @SerialName("reply") val reply: Reply? = null,
        ) : Message {
            @Serializable
            data class Reply(
                val author: RoomMember,
                val originalMessage: String,
                val replyContent: String,
                val eventId: EventId,
                val timestampUtc: Long,
            )
        }

        @Serializable
        @SerialName("image_message")
        data class ImageMessage(
            @SerialName("content") val content: Content.ImageContent,
            @SerialName("send_encrypted") val sendEncrypted: Boolean,
            @SerialName("room_id") val roomId: RoomId,
            @SerialName("local_id") val localId: String,
            @SerialName("timestamp") val timestampUtc: Long,
        ) : Message

        @Serializable
        sealed class Content {
            @Serializable
            data class TextContent(
                @SerialName("body") val body: String,
                @SerialName("msgtype") val type: String = MessageType.TEXT.value,
            ) : Content()

            @Serializable
            data class ImageContent(
                @SerialName("uri") val uri: String,
                @SerialName("meta") val meta: Meta,
            ) : Content() {

                @Serializable
                data class Meta(
                    val height: Int,
                    val width: Int,
                    val size: Long,
                    val fileName: String,
                    val mimeType: String,
                )

            }

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
            is Message.ImageMessage -> message.timestampUtc
        }

        @Transient
        val roomId = when (message) {
            is Message.TextMessage -> message.roomId
            is Message.ImageMessage -> message.roomId
        }

        @Transient
        val localId = when (message) {
            is Message.TextMessage -> message.localId
            is Message.ImageMessage -> message.localId
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
    imageContentReader: ImageContentReader,
    messageEncrypter: ServiceDepFactory<MessageEncrypter> = ServiceDepFactory { MissingMessageEncrypter },
    mediaEncrypter: ServiceDepFactory<MediaEncrypter> = ServiceDepFactory { MissingMediaEncrypter },
): InstallExtender<MessageService> {
    return this.install { (httpClient, _, installedServices) ->
        SERVICE_KEY to DefaultMessageService(
            httpClient,
            localEchoStore,
            backgroundScheduler,
            messageEncrypter.create(installedServices),
            mediaEncrypter.create(installedServices),
            imageContentReader
        )
    }
}

fun MatrixServiceProvider.messageService(): MessageService = this.getService(key = SERVICE_KEY)
