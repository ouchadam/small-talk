package app.dapk.st.matrix.message.internal

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.MessageType
import app.dapk.st.matrix.common.MxUrl
import app.dapk.st.matrix.common.RoomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiMessage {

    @Serializable
    @SerialName("text_message")
    data class TextMessage(
        @SerialName("content") val content: TextContent,
        @SerialName("room_id") val roomId: RoomId,
        @SerialName("type") val type: String,
    ) : ApiMessage() {

        @Serializable
        data class TextContent(
            @SerialName("body") val body: String,
            @SerialName("m.relates_to") val relatesTo: RelatesTo? = null,
            @SerialName("formatted_body") val formattedBody: String? = null,
            @SerialName("format") val format: String? = null,
        ) : ApiMessageContent {

            @SerialName("msgtype")
            val type: String = MessageType.TEXT.value
        }
    }

    @Serializable
    data class RelatesTo(
        @SerialName("m.in_reply_to") val inReplyTo: InReplyTo
    ) {

        @Serializable
        data class InReplyTo(
            @SerialName("event_id") val eventId: EventId
        )

    }

    @Serializable
    @SerialName("image_message")
    data class ImageMessage(
        @SerialName("content") val content: ImageContent,
        @SerialName("room_id") val roomId: RoomId,
        @SerialName("type") val type: String,
    ) : ApiMessage() {

        @Serializable
        data class ImageContent(
            @SerialName("url") val url: MxUrl?,
            @SerialName("body") val filename: String,
            @SerialName("info") val info: Info,
            @SerialName("msgtype") val type: String = MessageType.IMAGE.value,
            @SerialName("file") val file: File? = null,
        ) : ApiMessageContent {

            @Serializable
            data class Info(
                @SerialName("h") val height: Int,
                @SerialName("w") val width: Int,
                @SerialName("mimetype") val mimeType: String,
                @SerialName("size") val size: Long,
            )

            @Serializable
            data class File(
                @SerialName("url") val url: MxUrl,
                @SerialName("key") val key: EncryptionMeta,
                @SerialName("iv") val iv: String,
                @SerialName("hashes") val hashes: Map<String, String>,
                @SerialName("v") val v: String
            ) {
                @Serializable
                data class EncryptionMeta(
                    @SerialName("alg") val algorithm: String,
                    @SerialName("ext") val ext: Boolean,
                    @SerialName("key_ops") val keyOperations: List<String>,
                    @SerialName("kty") val kty: String,
                    @SerialName("k") val k: String
                )
            }
        }
    }
}

sealed interface ApiMessageContent
