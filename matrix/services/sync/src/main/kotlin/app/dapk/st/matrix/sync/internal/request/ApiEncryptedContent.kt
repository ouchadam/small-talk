package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.CipherText
import app.dapk.st.matrix.common.Curve25519
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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