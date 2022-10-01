package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.*

fun interface MessageEncrypter {

    suspend fun encrypt(message: ClearMessagePayload): EncryptedMessagePayload

    data class EncryptedMessagePayload(
        val algorithmName: AlgorithmName,
        val senderKey: String,
        val cipherText: CipherText,
        val sessionId: SessionId,
        val deviceId: DeviceId
    )

    data class ClearMessagePayload(
        val roomId: RoomId,
        val contents: JsonString,
    )
}

internal object MissingMessageEncrypter : MessageEncrypter {
    override suspend fun encrypt(message: MessageEncrypter.ClearMessagePayload) = throw IllegalStateException("No encrypter instance set")
}
