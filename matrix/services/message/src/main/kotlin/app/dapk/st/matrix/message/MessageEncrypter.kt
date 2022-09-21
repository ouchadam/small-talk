package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun interface MessageEncrypter {

    suspend fun encrypt(message: ClearMessagePayload): EncryptedMessagePayload

    @Serializable
    data class EncryptedMessagePayload(
        @SerialName("algorithm") val algorithmName: AlgorithmName,
        @SerialName("sender_key") val senderKey: String,
        @SerialName("ciphertext") val cipherText: CipherText,
        @SerialName("session_id") val sessionId: SessionId,
        @SerialName("device_id") val deviceId: DeviceId
    )

    data class ClearMessagePayload(
        val roomId: RoomId,
        val contents: JsonString,
    )
}

internal object MissingMessageEncrypter : MessageEncrypter {
    override suspend fun encrypt(message: MessageEncrypter.ClearMessagePayload) = throw IllegalStateException("No encrypter instance set")
}
