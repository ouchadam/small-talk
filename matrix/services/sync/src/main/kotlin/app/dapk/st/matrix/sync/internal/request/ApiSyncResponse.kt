package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class ApiSyncResponse(
    @SerialName("device_lists") val deviceLists: DeviceLists? = null,
    @SerialName("account_data") val accountData: ApiAccountData? = null,
    @SerialName("rooms") val rooms: ApiSyncRooms? = null,
    @SerialName("to_device") val toDevice: ToDevice? = null,
    @SerialName("device_one_time_keys_count") val oneTimeKeysCount: Map<String, ServerKeyCount>,
    @SerialName("next_batch") val nextBatch: SyncToken,
    @SerialName("prev_batch") val prevBatch: SyncToken? = null,
)

@Serializable
data class ApiAccountData(
    @SerialName("events") val events: List<ApiAccountEvent>
)

@Serializable
sealed class ApiAccountEvent {

    @Serializable
    @SerialName("m.direct")
    data class Direct(
        @SerialName("content") val content: Map<UserId, List<RoomId>>
    ) : ApiAccountEvent()

    @Serializable
    @SerialName("m.fully_read")
    data class FullyRead(
        @SerialName("content") val content: Content,
    ) : ApiAccountEvent() {

        @Serializable
        data class Content(
            @SerialName("event_id") val eventId: EventId,
        )

    }

    @Serializable
    object Ignored : ApiAccountEvent()
}

@Serializable
internal data class DeviceLists(
    @SerialName("changed") val changed: List<UserId>? = null
)

@Serializable
internal data class ToDevice(
    @SerialName("events") val events: List<ApiToDeviceEvent>
)

@Serializable
sealed class ApiToDeviceEvent {

    @Serializable
    @SerialName("m.room.encrypted")
    internal data class Encrypted(
        @SerialName("sender") val senderId: UserId,
        @SerialName("content") val content: ApiEncryptedContent,
    ) : ApiToDeviceEvent()

    @Serializable
    @SerialName("m.room_key")
    data class RoomKey(
        @SerialName("sender") val sender: UserId,
        @SerialName("content") val content: Content,
    ) : ApiToDeviceEvent() {
        @Serializable
        data class Content(
            @SerialName("room_id") val roomId: RoomId,
            @SerialName("algorithm") val algorithmName: AlgorithmName,
            @SerialName("session_id") val sessionId: SessionId,
            @SerialName("session_key") val sessionKey: String,
            @SerialName("chain_index") val chainIndex: Long,
        )
    }

    @Serializable
    @SerialName("m.key.verification.request")
    data class VerificationRequest(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("from_device") val fromDevice: DeviceId,
            @SerialName("methods") val methods: List<String>,
            @SerialName("timestamp") val timestampPosix: Long,
            @SerialName("transaction_id") val transactionId: String,
        )
    }

    @Serializable
    @SerialName("m.key.verification.ready")
    data class VerificationReady(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("from_device") val fromDevice: DeviceId,
            @SerialName("methods") val methods: List<String>,
            @SerialName("transaction_id") val transactionId: String,
        )
    }

    @Serializable
    @SerialName("m.key.verification.start")
    data class VerificationStart(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("from_device") val fromDevice: DeviceId,
            @SerialName("method") val method: String,
            @SerialName("key_agreement_protocols") val protocols: List<String>,
            @SerialName("hashes") val hashes: List<String>,
            @SerialName("message_authentication_codes") val codes: List<String>,
            @SerialName("short_authentication_string") val short: List<String>,
            @SerialName("transaction_id") val transactionId: String,
        )
    }

    @Serializable
    @SerialName("m.key.verification.accept")
    data class VerificationAccept(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("from_device") val fromDevice: DeviceId,
            @SerialName("method") val method: String,
            @SerialName("key_agreement_protocol") val protocol: String,
            @SerialName("hash") val hash: String,
            @SerialName("message_authentication_code") val code: String,
            @SerialName("short_authentication_string") val short: List<String>,
            @SerialName("commitment") val commitment: String,
            @SerialName("transaction_id") val transactionId: String,
        )
    }

    @Serializable
    @SerialName("m.key.verification.key")
    data class VerificationKey(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("transaction_id") val transactionId: String,
            @SerialName("key") val key: String,
        )
    }

    @Serializable
    @SerialName("m.key.verification.mac")
    data class VerificationMac(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("transaction_id") val transactionId: String,
            @SerialName("keys") val keys: String,
            @SerialName("mac") val mac: Map<String, String>,
        )
    }

    @Serializable
    @SerialName("m.key.verification.cancel")
    data class VerificationCancel(
        @SerialName("content") val content: Content,
    ) : ApiToDeviceEvent(), ApiVerificationEvent {

        @Serializable
        data class Content(
            @SerialName("code") val code: String,
            @SerialName("reason") val reason: String,
            @SerialName("transaction_id") val transactionId: String,
        )
    }

    @Serializable
    object Ignored : ApiToDeviceEvent()


    sealed interface ApiVerificationEvent
}

@Serializable
internal data class ApiSyncRooms(
    @SerialName("join") val join: Map<RoomId, ApiSyncRoom>? = null,
    @SerialName("invite") val invite: Map<RoomId, ApiSyncRoomInvite>? = null,
    @SerialName("leave") val leave: Map<RoomId, ApiSyncRoom>? = null,
)

@Serializable
internal data class ApiSyncRoomInvite(
    @SerialName("invite_state") val state: ApiInviteEvents,
)

@Serializable
internal data class ApiInviteEvents(
    @SerialName("events") val events: List<ApiStrippedEvent>
)

@Serializable
sealed class ApiStrippedEvent {

    @Serializable
    @SerialName("m.room.member")
    internal data class RoomMember(
        @SerialName("content") val content: Content,
        @SerialName("sender") val sender: UserId,
    ) : ApiStrippedEvent() {

        @Serializable
        internal data class Content(
            @SerialName("displayname") val displayName: String? = null,
            @SerialName("membership") val membership: ApiTimelineEvent.RoomMember.Content.Membership? = null,
            @SerialName("is_direct") val isDirect: Boolean? = null,
            @SerialName("avatar_url") val avatarUrl: MxUrl? = null,
        )
    }

    @Serializable
    @SerialName("m.room.name")
    internal data class RoomName(
        @SerialName("content") val content: Content,
    ) : ApiStrippedEvent() {

        @Serializable
        internal data class Content(
            @SerialName("name") val name: String? = null
        )
    }

    @Serializable
    object Ignored : ApiStrippedEvent()
}

@Serializable
internal data class ApiSyncRoom(
    @SerialName("timeline") val timeline: ApiSyncRoomTimeline,
    @SerialName("state") val state: ApiSyncRoomState,
    @SerialName("account_data") val accountData: ApiAccountData? = null,
    @SerialName("ephemeral") val ephemeral: ApiEphemeral? = null,
)

@Serializable
internal data class ApiEphemeral(
    @SerialName("events") val events: List<ApiEphemeralEvent>
)

@Serializable
internal sealed class ApiEphemeralEvent {

    @Serializable
    @SerialName("m.typing")
    internal data class Typing(
        @SerialName("content") val content: Content,
    ) : ApiEphemeralEvent() {
        @Serializable
        internal data class Content(
            @SerialName("user_ids") val userIds: List<UserId>
        )
    }

    @Serializable
    object Ignored : ApiEphemeralEvent()
}


@Serializable
internal data class ApiSyncRoomState(
    @SerialName("events") val stateEvents: List<ApiTimelineEvent>,
)

@Serializable
internal data class ApiSyncRoomTimeline(
    @SerialName("events") val apiTimelineEvents: List<ApiTimelineEvent>,
)


@Serializable
internal sealed class DecryptedContent {

    @Serializable
    @SerialName("m.room.message")
    internal data class TimelineText(
        @SerialName("content") val content: ApiTimelineEvent.TimelineMessage.Content,
    ) : DecryptedContent()

    @Serializable
    object Ignored : DecryptedContent()
}


@Serializable(with = EncryptedContentDeserializer::class)
internal sealed class ApiEncryptedContent {
    @Serializable
    data class OlmV1(
        @SerialName("ciphertext") val cipherText: Map<Curve25519, CipherTextInfo>,
        @SerialName("sender_key") val senderKey: Curve25519,
    ) : ApiEncryptedContent()

    @Serializable
    data class MegOlmV1(
        @SerialName("ciphertext") val cipherText: CipherText,
        @SerialName("device_id") val deviceId: DeviceId,
        @SerialName("sender_key") val senderKey: String,
        @SerialName("session_id") val sessionId: SessionId,
        @SerialName("m.relates_to") val relation: ApiTimelineEvent.TimelineMessage.Relation? = null,
    ) : ApiEncryptedContent()

    @Serializable
    data class CipherTextInfo(
        @SerialName("body") val body: CipherText,
        @SerialName("type") val type: Int,
    )

    @Serializable
    object Unknown : ApiEncryptedContent()
}

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
        )
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
                @SerialName("info") val info: Info,
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
                    @SerialName("h") val height: Int,
                    @SerialName("w") val width: Int,
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


internal object EncryptedContentDeserializer : KSerializer<ApiEncryptedContent> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("encryptedContent")

    override fun deserialize(decoder: Decoder): ApiEncryptedContent {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (val algorithm = element.jsonObject["algorithm"]?.jsonPrimitive?.content) {
            "m.olm.v1.curve25519-aes-sha2" -> ApiEncryptedContent.OlmV1.serializer().deserialize(decoder)
            "m.megolm.v1.aes-sha2" -> ApiEncryptedContent.MegOlmV1.serializer().deserialize(decoder)
            null -> ApiEncryptedContent.Unknown
            else -> throw IllegalArgumentException("Unknown algorithm : $algorithm")
        }
    }

    override fun serialize(encoder: Encoder, value: ApiEncryptedContent) = TODO("Not yet implemented")

}

internal object ApiTimelineMessageContentDeserializer : KSerializer<ApiTimelineEvent.TimelineMessage.Content> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("messageContent")

    override fun deserialize(decoder: Decoder): ApiTimelineEvent.TimelineMessage.Content {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (element.jsonObject["msgtype"]?.jsonPrimitive?.content) {
            "m.text" -> ApiTimelineEvent.TimelineMessage.Content.Text.serializer().deserialize(decoder)
            "m.image" -> ApiTimelineEvent.TimelineMessage.Content.Image.serializer().deserialize(decoder)
            else -> {
                println(element)
                ApiTimelineEvent.TimelineMessage.Content.Ignored
            }
        }
    }

    override fun serialize(encoder: Encoder, value: ApiTimelineEvent.TimelineMessage.Content) = when (value) {
        ApiTimelineEvent.TimelineMessage.Content.Ignored -> {}
        is ApiTimelineEvent.TimelineMessage.Content.Image -> ApiTimelineEvent.TimelineMessage.Content.Image.serializer().serialize(encoder, value)
        is ApiTimelineEvent.TimelineMessage.Content.Text -> ApiTimelineEvent.TimelineMessage.Content.Text.serializer().serialize(encoder, value)
    }

}