package app.dapk.st.matrix.sync

import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.matrix.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class RoomState(
    val roomOverview: RoomOverview,
    val events: List<RoomEvent>,
)

internal val DEFAULT_ZONE = ZoneId.systemDefault()
internal val MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Serializable
sealed class RoomEvent {

    abstract val eventId: EventId
    abstract val utcTimestamp: Long

    @Serializable
    @SerialName("message")
    data class Message(
        @SerialName("event_id") override val eventId: EventId,
        @SerialName("timestamp") override val utcTimestamp: Long,
        @SerialName("content") val content: String,
        @SerialName("author") val author: RoomMember,
        @SerialName("meta") val meta: MessageMeta,
        @SerialName("encrypted_content") val encryptedContent: MegOlmV1? = null,
        @SerialName("edited") val edited: Boolean = false,
    ) : RoomEvent() {

        @Serializable
        data class MegOlmV1(
            @SerialName("ciphertext") val cipherText: CipherText,
            @SerialName("device_id") val deviceId: DeviceId,
            @SerialName("sender_key") val senderKey: String,
            @SerialName("session_id") val sessionId: SessionId,
        )

        @Transient
        val time: String by unsafeLazy {
            val instant = Instant.ofEpochMilli(utcTimestamp)
            ZonedDateTime.ofInstant(instant, DEFAULT_ZONE).toLocalTime().format(MESSAGE_TIME_FORMAT)
        }
    }

    @Serializable
    @SerialName("reply")
    data class Reply(
        @SerialName("message") val message: Message,
        @SerialName("in_reply_to") val replyingTo: Message,
    ) : RoomEvent() {

        override val eventId: EventId = message.eventId
        override val utcTimestamp: Long = message.utcTimestamp

        val replyingToSelf = replyingTo.author == message.author

        @Transient
        val time: String by unsafeLazy {
            val instant = Instant.ofEpochMilli(utcTimestamp)
            ZonedDateTime.ofInstant(instant, DEFAULT_ZONE).toLocalTime().format(MESSAGE_TIME_FORMAT)
        }
    }

}

@Serializable
sealed class MessageMeta {

    @Serializable
    @SerialName("from_server")
    object FromServer : MessageMeta()

    @Serializable
    @SerialName("local_echo")
    data class LocalEcho(
        @SerialName("echo_id") val echoId: String,
        @SerialName("state") val state: State
    ) : MessageMeta() {

        @Serializable
        sealed class State {
            @Serializable
            @SerialName("loading")
            object Sending : State()

            @Serializable
            @SerialName("success")
            object Sent : State()

            @SerialName("error")
            @Serializable
            data class Error(
                @SerialName("message") val message: String,
                @SerialName("type") val type: Type,
            ) : State() {

                @Serializable
                enum class Type {
                    UNKNOWN
                }
            }
        }
    }
}