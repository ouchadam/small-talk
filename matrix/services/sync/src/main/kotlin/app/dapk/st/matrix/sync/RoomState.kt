package app.dapk.st.matrix.sync

import app.dapk.st.matrix.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RoomState(
    val roomOverview: RoomOverview,
    val events: List<RoomEvent>,
)

@Serializable
sealed class RoomEvent {

    abstract val eventId: EventId
    abstract val utcTimestamp: Long
    abstract val author: RoomMember
    abstract val meta: MessageMeta

    @Serializable
    @SerialName("encrypted")
    data class Encrypted(
        @SerialName("event_id") override val eventId: EventId,
        @SerialName("timestamp") override val utcTimestamp: Long,
        @SerialName("author") override val author: RoomMember,
        @SerialName("meta") override val meta: MessageMeta,
        @SerialName("edited") val edited: Boolean = false,
        @SerialName("encrypted_content") val encryptedContent: MegOlmV1,
    ) : RoomEvent() {

        @Serializable
        data class MegOlmV1(
            @SerialName("ciphertext") val cipherText: CipherText,
            @SerialName("device_id") val deviceId: DeviceId,
            @SerialName("sender_key") val senderKey: String,
            @SerialName("session_id") val sessionId: SessionId,
        )

    }

    @Serializable
    @SerialName("redacted")
    data class Redacted(
        @SerialName("event_id") override val eventId: EventId,
        @SerialName("timestamp") override val utcTimestamp: Long,
        @SerialName("author") override val author: RoomMember,
    ) : RoomEvent() {
        override val meta: MessageMeta = MessageMeta.FromServer
    }

    @Serializable
    @SerialName("message")
    data class Message(
        @SerialName("event_id") override val eventId: EventId,
        @SerialName("timestamp") override val utcTimestamp: Long,
        @SerialName("content") val content: RichText,
        @SerialName("author") override val author: RoomMember,
        @SerialName("meta") override val meta: MessageMeta,
        @SerialName("edited") val edited: Boolean = false,
    ) : RoomEvent()

    @Serializable
    @SerialName("reply")
    data class Reply(
        @SerialName("message") val message: RoomEvent,
        @SerialName("in_reply_to") val replyingTo: RoomEvent,
    ) : RoomEvent() {

        override val eventId: EventId = message.eventId
        override val utcTimestamp: Long = message.utcTimestamp
        override val author: RoomMember = message.author
        override val meta: MessageMeta = message.meta

    }

    @Serializable
    @SerialName("image")
    data class Image(
        @SerialName("event_id") override val eventId: EventId,
        @SerialName("timestamp") override val utcTimestamp: Long,
        @SerialName("image_meta") val imageMeta: ImageMeta,
        @SerialName("author") override val author: RoomMember,
        @SerialName("meta") override val meta: MessageMeta,
        @SerialName("edited") val edited: Boolean = false,
    ) : RoomEvent() {

        @Serializable
        data class ImageMeta(
            @SerialName("width") val width: Int?,
            @SerialName("height") val height: Int?,
            @SerialName("url") val url: String,
            @SerialName("keys") val keys: Keys?,
        ) {

            @Serializable
            data class Keys(
                @SerialName("k") val k: String,
                @SerialName("iv") val iv: String,
                @SerialName("v") val v: String,
                @SerialName("hashes") val hashes: Map<String, String>,
            )

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