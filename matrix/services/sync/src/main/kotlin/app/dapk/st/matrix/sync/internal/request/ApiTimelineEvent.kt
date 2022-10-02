package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.MxUrl
import app.dapk.st.matrix.common.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ApiTimelineEvent {

    @Serializable
    @SerialName("m.room.create")
    internal data class RoomCreate(
        @SerialName("event_id") val id: EventId,
        @SerialName("origin_server_ts") val utcTimestamp: Long,
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("type") val type: String? = null
        ) {

            object Type {
                const val SPACE = "m.space"
            }

        }
    }

    @Serializable
    @SerialName("m.room.topic")
    internal data class RoomTopic(
        @SerialName("event_id") val id: EventId,
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("topic") val topic: String
        )
    }

    @Serializable
    @SerialName("m.room.name")
    internal data class RoomName(
        @SerialName("event_id") val id: EventId,
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("name") val name: String
        )
    }

    @Serializable
    @SerialName("m.room.canonical_alias")
    internal data class CanonicalAlias(
        @SerialName("event_id") val id: EventId,
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("alias") val alias: String? = null
        )
    }

    @Serializable
    @SerialName("m.room.avatar")
    internal data class RoomAvatar(
        @SerialName("event_id") val id: EventId,
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("url") val url: MxUrl? = null
        )
    }

    @Serializable
    @SerialName("m.room.member")
    internal data class RoomMember(
        @SerialName("event_id") val id: EventId,
        @SerialName("content") val content: Content,
        @SerialName("sender") val senderId: UserId,
    ) : ApiTimelineEvent() {

        @Serializable
        internal data class Content(
            @SerialName("displayname") val displayName: String? = null,
            @SerialName("membership") val membership: Membership,
            @SerialName("avatar_url") val avatarUrl: MxUrl? = null,
        ) {

            @JvmInline
            @Serializable
            value class Membership(val value: String) {
                fun isJoin() = value == "join"
                fun isInvite() = value == "invite"
                fun isLeave() = value == "leave"
            }

        }
    }

    @Serializable
    @SerialName("m.room.redaction")
    internal data class RoomRedcation(
        @SerialName("event_id") val id: EventId,
        @SerialName("redacts") val redactedId: EventId,
        @SerialName("origin_server_ts") val utcTimestamp: Long,
        @SerialName("sender") val senderId: UserId,
    ) : ApiTimelineEvent()

    @Serializable
    internal data class DecryptionStatus(
        @SerialName("is_verified") val isVerified: Boolean
    )

    @Serializable
    @SerialName("m.room.message")
    internal data class TimelineMessage(
        @SerialName("event_id") val id: EventId,
        @SerialName("sender") val senderId: UserId,
        @SerialName("content") val content: Content,
        @SerialName("origin_server_ts") val utcTimestamp: Long,
        @SerialName("st.decryption_status") val decryptionStatus: DecryptionStatus? = null
    ) : ApiTimelineEvent() {

        @Serializable(with = ApiTimelineMessageContentDeserializer::class)
        internal sealed interface Content {
            val relation: Relation?

            @Serializable
            data class Text(
                @SerialName("body") val body: String? = null,
                @SerialName("formatted_body") val formattedBody: String? = null,
                @SerialName("m.relates_to") override val relation: Relation? = null,
                @SerialName("msgtype") val messageType: String = "m.text",
            ) : Content

            @Serializable
            data class Image(
                @SerialName("url") val url: MxUrl? = null,
                @SerialName("file") val file: File? = null,
                @SerialName("info") val info: Info? = null,
                @SerialName("m.relates_to") override val relation: Relation? = null,
                @SerialName("msgtype") val messageType: String = "m.image",
            ) : Content {

                @Serializable
                data class File(
                    @SerialName("url") val url: MxUrl,
                    @SerialName("iv") val iv: String,
                    @SerialName("v") val v: String,
                    @SerialName("hashes") val hashes: Map<String, String>,
                    @SerialName("key") val key: Key,
                ) {

                    @Serializable
                    data class Key(
                        @SerialName("k") val k: String,
                    )

                }

                @Serializable
                internal data class Info(
                    @SerialName("h") val height: Int? = null,
                    @SerialName("w") val width: Int? = null,
                )
            }

            @Serializable
            object Ignored : Content {
                override val relation: Relation? = null
            }
        }

        @Serializable
        data class Relation(
            @SerialName("m.in_reply_to") val inReplyTo: InReplyTo? = null,
            @SerialName("rel_type") val relationType: String? = null,
            @SerialName("event_id") val eventId: EventId? = null
        )

        @Serializable
        data class InReplyTo(
            @SerialName("event_id") val eventId: EventId
        )
    }


    @Serializable
    @SerialName("m.room.encryption")
    data class Encryption(
        @SerialName("content") val content: Content,
    ) : ApiTimelineEvent() {
        @Serializable
        data class Content(
            @SerialName("algorithm") val algorithm: AlgorithmName,
            @SerialName("rotation_period_ms") val rotationMs: Long? = null,
            @SerialName("rotation_period_msgs") val rotationMessages: Long? = null,
        )
    }

    @Serializable
    @SerialName("m.room.encrypted")
    internal data class Encrypted(
        @SerialName("sender") val senderId: UserId,
        @SerialName("content") val encryptedContent: ApiEncryptedContent,
        @SerialName("event_id") val eventId: EventId,
        @SerialName("origin_server_ts") val utcTimestamp: Long,
    ) : ApiTimelineEvent()

    @Serializable
    object Ignored : ApiTimelineEvent()
}