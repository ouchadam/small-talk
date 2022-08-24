package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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