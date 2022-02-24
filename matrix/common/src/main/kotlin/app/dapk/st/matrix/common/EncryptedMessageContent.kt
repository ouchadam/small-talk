package app.dapk.st.matrix.common

sealed class EncryptedMessageContent {

    data class OlmV1(
        val senderId: UserId,
        val cipherText: Map<Curve25519, CipherTextInfo>,
        val senderKey: Curve25519,
    ) : EncryptedMessageContent()

    data class MegOlmV1(
        val cipherText: CipherText,
        val deviceId: DeviceId,
        val senderKey: String,
        val sessionId: SessionId,
    ) : EncryptedMessageContent()

    data class CipherTextInfo(
        val body: CipherText,
        val type: Int,
    )
}